package robot1;

import robotUtil.MotorController;

public class Robot1 {

  private long getPID() {
    return Long.parseLong(java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
  }

  // -----------------------------------------------------------------------------------------------------------
  private class ShutdownHook implements Runnable{
    MotorController mc;

    ShutdownHook(MotorController mcArg){
      mc = mcArg;
    }

    @Override
    public void run() {
      try {
        System.out.println("Robot1.ShutdownHook: Shutdown hook started.");
        mc.shutdown();
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        System.out.println("Robot1.ShutdownHook: Shutdown hook completed.");
      }
    }
  }

  private void run() {
    MotorController mc = null;
    System.out.printf("Begin Robot1. My pid is %d.\n", getPID());
    try {

      // Setup the motor controller.
      mc = new MotorController();

      // Add a shutdown hook which stops the motor-controller and scanner.
      Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHook(mc)));

      // Added code here.
      mc.speed(75);

      mc.forwardSteps(13402);
      mc.waitMovement();
      mc.left(2000);
      mc.waitMovement();
      mc.forwardSteps(13402);
      mc.waitMovement();
      mc.left(3000);
      mc.waitMovement();
      mc.forwardSteps(18953);
      mc.waitMovement();
      mc.left(3000);
      mc.waitMovement();

      // Quit time.
      mc.shutdown();
    } catch (Exception e) {
      System.out.println("run: exception occurred; exception= " + e.getMessage());
      e.printStackTrace();
    } finally {
      // Quit time. Watch for NULL references; unlikely but possible.
      if(mc != null){mc.shutdown();}
      System.out.println("End of Robot1.");
    }
  }

  public static void main(String[] args) {
    Robot1 application = new Robot1();
    application.run();
  }
}
