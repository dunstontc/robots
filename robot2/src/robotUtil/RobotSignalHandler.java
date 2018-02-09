package robotUtil;

import sun.misc.Signal;
import sun.misc.SignalHandler;

public class RobotSignalHandler implements SignalHandler {

  public static void listenTo(String name) {
    Signal signal = new Signal(name);
    Signal.handle(signal, new RobotSignalHandler());
  }

  public void handle(Signal signal) {
    System.out.println("Signal: " + signal);
    if (signal.toString().trim().equals("SIGTERM")) {
      System.out.println("SIGTERM raised.");
    }
  }

}
