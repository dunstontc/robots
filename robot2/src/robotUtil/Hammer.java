package robotUtil;

import java.lang.Math;

public class Hammer {

  public int FirstAngle;

  public int FirstDistance;
  public int SecondDistance;

  public int getSecondAngle() {
    return CalcSecondTurn(this.FirstDistance, this.SecondDistance);
  }

  public int getThirdDistance() {
    return GetD3(this.FirstDistance, this.SecondDistance);
  }

  public void VictoryDance() {
    System.out.print("\n ===  ᕕ( ᐛ )ᕗ  === \n");
  }

  private static int CalcSecondTurn(int d1, int d2) {
    // Compute tangent of angle.
    float tangent = d1 / d2;

    // Compute angle
    double theta = Math.atan(tangent); // angle in radians
    theta = theta * 57.2958;           // angle in degrees

    // Compute steps for second turn.
    int steps = (int) ((180.0 - theta) * 22.46);

    return steps;
  }

  private static int GetD3(int x, int y) {
    double c = Math.sqrt((x * x) + (y * y));
    return (int) c;
  }

}


//    // Compute tangent of angle.
//    float tangent = d1 /d2;
//    // Compute angle
//    double theta = atan(tangent); // angle in radians
//    theta = theta * 57.2958;      // angle in degrees
//    // Compute steps for second turn.
//    int steps = (int) ((180.0 - theta) * 22.46);