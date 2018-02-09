# 2018-02-09

## Robot2

Write a program which directs the robot to move forward until it is 15cm from the front obstacle.
It should then turn 90 degrees to the left and move forward until it is 15cm from the front obstacle. 
The robot should then return to the original starting point.

- Variable number of steps
- scanner..
  - Use measurements[11] to determine if we need to stop
- getMcRegistry() - 1 reg for each wheel, tracks steps
  - measurements[0] = steps
