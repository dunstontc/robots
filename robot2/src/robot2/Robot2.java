package robot2;

import java.util.concurrent.TimeUnit;


import robotUtil.Hammer;
import robotUtil.MotorController;
import robotUtil.Scanner;


public class Robot2 {

  // -----------------------------------------------------------------------------------------------------------
  private long getPID() {
    return Long.parseLong(java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
  }

  // -----------------------------------------------------------------------------------------------------------
  private class ShutdownHook implements Runnable {
    MotorController mc;
    Scanner scanner;
    Thread scannerThread;

    ShutdownHook(MotorController mcArg, Scanner scannerArg, Thread scannerThreadArg) {
      mc = mcArg;
      scanner = scannerArg;
      scannerThread = scannerThreadArg;
    }

    @Override
    public void run() {
      try {
        System.out.println("Robot2.ShutdownHook: Shutdown hook started.");
        mc.shutdown();
        if (scannerThread.isAlive()) {
          scanner.shutdown();
          scannerThread.join();
        }
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        System.out.println("Robot2.ShutdownHook: Shutdown hook completed.");
      }
    }
  }

  // -----------------------------------------------------------------------------------------------------------
  private void run() {
    MotorController mc = null;
    Scanner scanner = null;
    Thread scannerThread = null;
    System.out.printf("Begin Robot2. My pid is %d.\n", getPID());
    try {
      // Setup the motor controller.
      mc = new MotorController();

      // Setup the scanner.
      scanner = new Scanner();
      scannerThread = new Thread(scanner);
      scannerThread.start();

      // Add a shutdown hook which stops the motor-controller and scanner.
      Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHook(mc, scanner, scannerThread)));

      // Allow time for scanner to start.
      TimeUnit.SECONDS.sleep(1);

      // -----------------------------------------------------------------------------------------------------------
      Hammer thisHammer = new Hammer();


      // === First Section ===
      mc.speed(100);
      mc.forward();
      while (scanner.getMeasurement()[11] > 150) {
        System.out.printf("First Distance: %d %n", scanner.getMeasurement()[11]);
        if (scanner.getMeasurement()[11] < 600) {
          mc.speed(10);
        }
      }

      mc.stop();
      mc.speed(100);

      thisHammer.FirstAngle = mc.getMCRegisters()[0];

      mc.left(thisHammer.FirstAngle);
      mc.waitMovement();


      // === Second Section ===
      mc.speed(100);

      mc.forward();
      while (scanner.getMeasurement()[11] > 155) {
        System.out.printf("Second Distance: %d %n", scanner.getMeasurement()[11]);
        if (scanner.getMeasurement()[11] < 600) {
          mc.speed(10);
        }
      }

      mc.stop();
      mc.speed(100);

      thisHammer.SecondDistance = mc.getMCRegisters()[0];

      mc.left(thisHammer.getSecondAngle());
      mc.waitMovement();


      // === Third Section ===
      mc.forwardSteps(thisHammer.getThirdDistance());
      mc.waitMovement();

      mc.left(thisHammer.getSecondAngle());
      mc.waitMovement();


      // === Victory Dance ===
      mc.right(8000);
      mc.waitMovement();
      thisHammer.VictoryDance();

    } catch (Exception e) {
      System.out.println("run: exception occurred; exception= " + e.getMessage());
      e.printStackTrace();
    } finally {
      // Quit time. Watch for NULL references; unlikely but possible.
      if (mc != null) {
        mc.shutdown();
      }
      if (scanner != null) {
        scanner.shutdown();
        if (scannerThread != null) {
          try {
            scannerThread.join();
          } catch (InterruptedException e) {
            // Do nothing since we are trying to terminate. Just report the problem so we can investigate.
            System.out.println("End of Robot1.");
          }
        }
      }
      System.out.println("End of Robot2.");
    }
  }

  // -----------------------------------------------------------------------------------------------------------
  public static void main(String[] args) {
    Robot2 application = new Robot2();
    application.run();
  }
}
