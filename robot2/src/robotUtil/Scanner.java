package robotUtil;

import com.fazecast.jSerialComm.SerialPort;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Scanner implements Runnable{

  private SerialPort comPort;
  private byte[] responseData= null;
  private final Lock getResponseLock = new ReentrantLock();
  private final Lock responseDataLock = new ReentrantLock();
  private boolean doMeasurements;

  //----------------------------------------------------------------------------------------------------------------------
  private class ResultContainer {
    String resultString;
    String errorMessage;
    byte[] byteArray;

    ResultContainer() {
      resultString = null;
      errorMessage = null;
      byteArray = null;
    }
  }

  //----------------------------------------------------------------------------------------------------------------------
  private ResultContainer getResponse() {
    ResultContainer result = new ResultContainer();
    byte[] responseByte = new byte[1];
    ByteArrayOutputStream response = new ByteArrayOutputStream();
    boolean repeat = true;
    boolean sawNL = false;
    getResponseLock.lock();
    while (repeat) {
      int bytesRead = comPort.readBytes(responseByte, 1);
      if (bytesRead == 1) {
        try {
          response.write(responseByte);
          if (responseByte[0] == 0xa) {
//            System.out.printf("====> newLine sawNL=%b\n", sawNL);
            if (sawNL) {
              repeat = false;
            } else {
              sawNL = true;
            }
          } else {
            sawNL = false;
          }
//          System.out.printf("---->%d  sawNL=%b  repeat=%b\n", responseByte[0], sawNL, repeat);
        } catch (IOException e) {
          result.errorMessage = "getResponse: response.write() failure.";
          repeat = false;
        }
      } else {
        result.errorMessage = "getResponse: comPort.readBytes() failure.";
        repeat = false;
      }
    }
    result.resultString = response.toString();
    result.byteArray = response.toByteArray();
    getResponseLock.unlock();
    return result;
  }

  //----------------------------------------------------------------------------------------------------------------------
  private void sendCommand(String cmd) throws Exception {
//    System.out.printf("====>%s\n", cmd);
    byte[] byteCmd = cmd.getBytes();
    int bytesWritten = comPort.writeBytes(byteCmd, byteCmd.length);
    if (bytesWritten != byteCmd.length) {
      throw new Exception("sendCommand: comPort.writeBytes() failure.");
    }
  }

  //----------------------------------------------------------------------------------------------------------------------
  private List<byte[]> split(byte[] array, byte[] delimiter) {
    List<byte[]> byteArrays = new LinkedList<>();
    if (delimiter.length == 0) {
      return byteArrays;
    }
    int begin = 0;

    outer:
    for (int i = 0; i < array.length - delimiter.length + 1; i++) {
      for (int j = 0; j < delimiter.length; j++) {
        if (array[i + j] != delimiter[j]) {
          continue outer;
        }
      }

      // If delimiter is at the beginning then there will not be any data.
      if (begin != i)
        byteArrays.add(Arrays.copyOfRange(array, begin, i));
      begin = i + delimiter.length;
    }

    // delimiter at the very end with no data following?
    if (begin != array.length)
      byteArrays.add(Arrays.copyOfRange(array, begin, array.length));

    return byteArrays;
  }

  //----------------------------------------------------------------------------------------------------------------------
  private Integer[] getMeasurements(byte[] response){
    ArrayList<Integer> measurementList= new ArrayList<>();
    byte[] delimiter= new byte[]{10};
//    System.out.printf("getMeasurements: response.length=%d.\n", response.length);
    List<byte[]> segments= split(response, delimiter);
//    System.out.printf("getMeasurements: segments.size()=%d.\n", segments.size());
    // Must see '99b' in second segment.
    if(!(segments.get(1)[0] == 57 && segments.get(1)[1] == 57 && segments.get(1)[2] == 98)){
      System.out.printf("No measurement data available; bad status. status=%c%c.\n",segments.get(1)[0],segments.get(1)[1]);
    }else{
      ByteArrayOutputStream rawData = new ByteArrayOutputStream();
      // Place only the data segments into 'rawData'.
      for(int index= 3; index < segments.size(); index +=1){
        byte[] segment= segments.get(index);
        rawData.write(segment, 0, segment.length-1); // Remove 'sum' byte from end of a segment.
      }
      // Extract 'dataArray' from 'rawData'.
      byte[] dataArray= rawData.toByteArray();
      // Convert the encoded data (3 bytes per measurement) into integer values (millimeters).
      for(int index= 0; index+2 < dataArray.length; index += 3){
        int measurement= (short)(dataArray[index] - 0x30) << 12 | (short)(dataArray[index+1] - 0x30) << 6 | (short)(dataArray[index+2] - 0x30);
        measurementList.add(measurement);
      }
    }
    return measurementList.toArray(new Integer[measurementList.size()]);
  }

  //----------------------------------------------------------------------------------------------------------------------
  private void xeqMSCmd() throws Exception {
    int startStep = 44; // right side
    int lastStep = 725; // left side
    int clusterCount = 31;
    int scanInterval = 1;
    int numOfScans = 0;
    String cmdString = String.format("MD%04d%04d%02d%d%02d\n", startStep, lastStep, clusterCount, scanInterval, numOfScans);

    getResponseLock.lock();
    getResponseLock.unlock();

    sendCommand(cmdString);
    ResultContainer result = getResponse();
    if (result.errorMessage != null) {
      throw new Exception("xeqMSCmd: sendCommand() failure; error= " + result.errorMessage);
    }

    while(doMeasurements){
      result = getResponse();
      if (result.errorMessage == null) {
        responseDataLock.lock();
        responseData = result.byteArray;
        responseDataLock.unlock();
      }
    }
  }

  //----------------------------------------------------------------------------------------------------------------------
  private void xeqResetCmd() throws Exception {
    getResponseLock.lock();
    getResponseLock.unlock();

    sendCommand("RS\n");
    ResultContainer result = getResponse();
    if (result.errorMessage != null) {
      throw new Exception("xeqResetCmd: sendCommand() failure; error= " + result.errorMessage);
    }

  }

  //----------------------------------------------------------------------------------------------------------------------
  public void shutdown() {
    doMeasurements = false;
    try {
      xeqResetCmd();
    } catch (Exception e) { // Catch any exception so that shutdown does not have to propagate an Exception.
      // Do nothing.
    }
    comPort.closePort();
  }

  //----------------------------------------------------------------------------------------------------------------------
  public Scanner() throws Exception {
    comPort = SerialPort.getCommPort("/dev/ttyACM0");
    comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
    if (comPort.openPort()) {
      sendCommand("RS\n");
      ResultContainer result = getResponse();
      if (result.errorMessage != null) {
        throw new Exception("Scanner: getResponse() failure.");
      }
    } else {
      throw new Exception("Scanner: Open port failure.");
    }

  }

  //----------------------------------------------------------------------------------------------------------------------
  public Integer[] getMeasurement() throws Exception{
    byte[] r= null;
    while(r == null){
      if (responseData == null) {
        TimeUnit.MILLISECONDS.sleep(10);
      }else{
        responseDataLock.lock(); // Lock before using the responseData.
        r= Arrays.copyOf(responseData, responseData.length);
        responseData= null;
        responseDataLock.unlock(); // Unlock.
      }
    }
    return getMeasurements(r);
  }

  //----------------------------------------------------------------------------------------------------------------------
  @Override
  public void run() {
    doMeasurements = true;
    try {
      xeqMSCmd();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
