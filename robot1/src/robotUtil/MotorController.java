package robotUtil;

import com.fazecast.jSerialComm.SerialPort;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MotorController {
  private final int minSpeed = 100;
  private final int maxSpeed = 3000;

  private SerialPort comPort;

  //----------------------------------------------------------------------------------------------------------------------
  // Define a simple container class.
  private class ResultContainer {
    String resultString;
    String errorMessage;

    ResultContainer() {
      resultString = null;
      errorMessage = null;
    }
  }

  //----------------------------------------------------------------------------------------------------------------------
  // Get a response from the motor controller.
  private ResultContainer getResponse() {
    ResultContainer result = new ResultContainer();
    byte[] responseByte = new byte[1];
    ByteArrayOutputStream response = new ByteArrayOutputStream();
    boolean repeat = true;
    while (repeat) {
      int bytesRead = comPort.readBytes(responseByte, 1);
      if (bytesRead == 1) {
        try {
          response.write(responseByte);
          if (responseByte[0] == 0x2a) {
            repeat = false;
          }
//          System.out.printf("getResponse: ---->%c  %d  %b\n", responseByte[0], responseByte[0], repeat);
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
    return result;
  }

  //----------------------------------------------------------------------------------------------------------------------
  // Send a command to the motor controller.
  private ResultContainer sendCommand(String cmd) throws Exception {
    ResultContainer result;
//    System.out.printf("====>%s\n", cmd);
    byte[] byteCmd = cmd.getBytes();
    int bytesWritten = comPort.writeBytes(byteCmd, byteCmd.length);
    if (bytesWritten != byteCmd.length) {
      throw new Exception("sendCommand: comPort.writeBytes() failure.");
    } else {
      result = getResponse();
      if (result.errorMessage == null) {
        if (!result.resultString.contains("*")) {
          throw new Exception("sendCommand: Unexpected command response; response= " + result.resultString);
        }
      }
    }
    return result;
  }

  //----------------------------------------------------------------------------------------------------------------------
  // Reset the motor controller registers.
  private void resetMCRegisters() throws Exception {
    sendCommand("B0=");
  }

  //----------------------------------------------------------------------------------------------------------------------
  // Get the motor controller registers.
  public int[] getMCRegisters() throws Exception {
    int[] valueArray = new int[2];
    ResultContainer result = sendCommand("B-1?");
    if (result.errorMessage != null) {
      throw new Exception("getMCRegisters: getResponse() failure; errorMessage= " + result.errorMessage);
    }
    Pattern p = Pattern.compile("\r*\nX[,][-]1[,][-]*([0-9]+)\r\nY[,][-]1[,][-]*([0-9]+)\r\n[*]");
    Matcher m = p.matcher(result.resultString);
    if(m.matches() && m.groupCount() == 2){
      valueArray[0] = Integer.decode(m.group(1));
      valueArray[1] = Integer.decode(m.group(2));
    }else{
      throw new Exception("getMCRegisters: m.matches() failure.");
    }
    return valueArray;
  }

  //----------------------------------------------------------------------------------------------------------------------
  // Reset the motor controller.
  public void reset() throws Exception {
    sendCommand("!");
  }

  //----------------------------------------------------------------------------------------------------------------------
  // Stop the motors.
  public void stop() throws Exception {
    if(comPort.isOpen()){
      sendCommand("BZ");
    }
  }

  //----------------------------------------------------------------------------------------------------------------------
  // Turn left a specified number of steps.
  public void left(int steps) throws Exception {
    String stepsStr = Integer.toString(steps);
    resetMCRegisters();
    sendCommand("B" + stepsStr + "g");
  }

  //----------------------------------------------------------------------------------------------------------------------
  // Turn right a specified number of steps.
  public void right(int steps) throws Exception {
    String stepsStr = Integer.toString(steps);
    resetMCRegisters();
    sendCommand("B-" + stepsStr + "g");
  }

  //----------------------------------------------------------------------------------------------------------------------
  // Move forward a specified number of steps.
  public void forwardSteps(int steps) throws Exception {
    String stepsStr = Integer.toString(steps);
    resetMCRegisters();
    sendCommand("x-" + stepsStr + "g");
    sendCommand("y" + stepsStr + "g");
  }

  //----------------------------------------------------------------------------------------------------------------------
  // Move forward.
  public void forward() throws Exception {
    resetMCRegisters();
    sendCommand("x-S");
    sendCommand("y+S");
  }

  //----------------------------------------------------------------------------------------------------------------------
  // Move backward a specified number of steps.
  public void backwardSteps(int steps) throws Exception {
    String stepsStr = Integer.toString(steps);
    resetMCRegisters();
    sendCommand("x" + stepsStr + "g");
    sendCommand("y-" + stepsStr + "g");
  }

  //----------------------------------------------------------------------------------------------------------------------
  // Move backwards.
  public void backward() throws Exception {
    resetMCRegisters();
    sendCommand("x+S");
    sendCommand("y-S");
  }

  //----------------------------------------------------------------------------------------------------------------------
  // Wait for movement to complete.
  public void waitMovement() throws Exception {
    sendCommand("BI");
  }

  //----------------------------------------------------------------------------------------------------------------------
  // Set the speed; expressed as a percentage of max.
  public void speed(int... percent) throws Exception {
    float speedRange = (float) (maxSpeed - minSpeed);
    float speedLeft = (speedRange * (float) (percent[0]) / 100.0F) + (float) (minSpeed);
    float speedRight = speedLeft;
    if (percent.length == 2) {
      speedRight = (speedRange * (float) (percent[1]) / 100.0F) + (float) (minSpeed);
    }
    sendCommand("x" + (int) speedLeft + "R");
    sendCommand("y" + (int) speedRight + "R");
  }

  //----------------------------------------------------------------------------------------------------------------------
  // Close our connection to the motor controller.
  public void shutdown() {
    if(comPort.isOpen()){
      try {
        this.stop(); // Safety stop in case the motors are running.
      } catch (Exception e) {
        // Do nothing since we are trying to shutdown. Just report the problem so we can investigate.
        System.out.println("shutdown: stop() failure; error="+e.getMessage());
      }
      comPort.closePort();
    }
  }

  //----------------------------------------------------------------------------------------------------------------------
  // Constructor for class.
  public MotorController() throws Exception {
    // Create our connection to the motor controller.
    comPort = SerialPort.getCommPort("/dev/ttyUSB0");
    comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
    if (comPort.openPort()) {
      // Initialization of motor controller.
      sendCommand("B");
      sendCommand(Integer.toString(minSpeed) + "R");
      sendCommand("300H");
      sendCommand("100W");
      sendCommand("2000P");
    } else {
      throw new Exception("MotorController: Open port failure.");
    }

  }
}
