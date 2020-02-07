
package frc.robot;

//Notes section
//may be two sparks later and/or spikes later on
//the importer was giving me trouble so I manually changed the current language to java and the project year to 2020 in the wpilib_preferences.json
//TO-DO: Add manual control to just about every aspect of the robot should something go wrong and also for debugging purposes
//TO-DO: Make rough estimations for distance in auto by taking the circumferance of the wheel and the rpm
//the circumference of the wheels is 18.849555921538 (diameter is 6) and the rpm is 5310rpm
//gear ratios are 5.392(motor rotations):1(wheel rotation) and 12.255(motor rotations): 1(wheel rotation)
//TO-DO the gear should be switched to have more torque in auto at the cost of speed for increased accuracy

//imports
import com.ctre.phoenix.motorcontrol.ControlMode;
//import com.ctre.phoenix.motorcontrol.can.VictorSPX;
import com.ctre.phoenix.motorcontrol.can.WPI_VictorSPX;
import com.kauailabs.navx.frc.AHRS;
import com.revrobotics.ColorMatch;
import com.revrobotics.ColorMatchResult;
import com.revrobotics.ColorSensorV3;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.I2C;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.util.Color;
import edu.wpi.first.wpilibj.SPI;
import edu.wpi.first.wpilibj.Timer;


public class Robot extends TimedRobot {

//_______________Declarations_______________

	//Hardware Declarations
	//note for the future, Victor is a part of the normal wpi, and the ctre library features the VictorSPX and the WPI_VictorSPX.
	//(cont.) Most of the corresponding imports are above but are commented out.
	WPI_VictorSPX FRMotor = new WPI_VictorSPX(1);
	WPI_VictorSPX BRMotor = new WPI_VictorSPX(2);
	WPI_VictorSPX FLMotor = new WPI_VictorSPX(3);
	WPI_VictorSPX BLMotor = new WPI_VictorSPX(4);
	WPI_VictorSPX ColorMotor = new WPI_VictorSPX(5);
	WPI_VictorSPX WinchMotor = new WPI_VictorSPX(6);
	WPI_VictorSPX SwifferMotor = new WPI_VictorSPX(7);
	WPI_VictorSPX BeltMotor = new WPI_VictorSPX(8);
	WPI_VictorSPX ExtendyBoi = new WPI_VictorSPX(12);
	DoubleSolenoid SwifferPiston = new DoubleSolenoid(9, 1, 2);
	DoubleSolenoid GearShift = new DoubleSolenoid(10, 3, 4);
	DoubleSolenoid CollectionDoor = new DoubleSolenoid(11, 5, 6);
	I2C.Port i2cPort = I2C.Port.kOnboard;
	ColorSensorV3 colorSensor = new ColorSensorV3(i2cPort);
	AHRS ahrs;

	//Color Sensor Declaration/Values
	final Color kBlueTarget = ColorMatch.makeColor(0.143, 0.427, 0.429);
	final Color kGreenTarget = ColorMatch.makeColor(0.197, 0.561, 0.240);
	final Color kRedTarget = ColorMatch.makeColor(0.561, 0.232, 0.114);
  	final Color kYellowTarget = ColorMatch.makeColor(0.361, 0.524, 0.113);
	ColorMatch m_colorMatcher = new ColorMatch();
	ColorMatchResult match;
	Color currentColor;
	String colorString;

	//Joystick declarations
	Joystick joyE = new Joystick(0);
	Joystick joyL = new Joystick(1);
	Joystick joyR = new Joystick(2);
	boolean joyETRigger;
	double RightVal;
	double LeftVal;
	double ExtraVal;
	boolean groundCollection;
	boolean ballShooter;
	boolean eject;
	boolean winchForwards;
	boolean winchReverse;
	int POVhook;
	boolean stationCollection;
	boolean colorRotations;
	boolean colorEndgame;
	boolean gearShift;
	boolean resetButton1;
	boolean resetButton2;
	int POVgearswitch;

	//Additional Values
	double ColorMotorVal = 0.5;
	int color = 0;
	int fieldColor = 0;
	int endgameTargetColor;
	int halfRotation = 0;
	boolean endRotation;
	boolean allRotationsDone = false;
	String gameData;
	Timer autoTimer = new Timer();
	double X;
	double Y;
	double Z;
	SendableChooser Position = new SendableChooser<>();
	SendableChooser Action = new SendableChooser<>();

	// This function is called once at the beginning during operator control
	public void robotInit() {
		try {
            ahrs = new AHRS(SPI.Port.kMXP); 
        } catch (RuntimeException ex ) {
            DriverStation.reportError("Error instantiating navX MXP:  " + ex.getMessage(), true);
		}
		m_colorMatcher.addColorMatch(kBlueTarget);
		m_colorMatcher.addColorMatch(kGreenTarget);
		m_colorMatcher.addColorMatch(kRedTarget);
		m_colorMatcher.addColorMatch(kYellowTarget);
	}

  	//Autonomous starts here, beware all ye who dare to look
  	public void autonomousInit() {
		//selecting the autonomous options
		Position.addOption("Left", 1);
		Position.addOption("Middle", 2);
		Position.addOption("Right", 3);
		Position.setDefaultOption("Left", 1);
		SmartDashboard.putData("Starting Position", Position);
		
		Action.addOption("Drive past starting line (makes position irrelevant)", 1);
		Action.addOption("Do nothing (makes position irrelevant)", 2);
		Action.addOption("Drive to trench (only available if Right side is selected", 3);
		SmartDashboard.putData("Where we droppin' bois", Action);
    	autoTimer.start();
    	GearShift.set(DoubleSolenoid.Value.kForward);
	}

  	public void autonomousPeriodic() {
		//lil' bit of setup code before the main event
		String SelectedAction = Action.getSelected().toString();
		String SelectedPosition = Action.getSelected().toString();
		double currentAutoTime = autoTimer.get();
		
		//Cross the starting line
		if (SelectedAction == "1") {
   		 	if (currentAutoTime<3) {
      			GearShift.set(DoubleSolenoid.Value.kForward);
      			FRMotor.set(0.25);
    	  		BRMotor.set(0.25);
      			FLMotor.set(-0.25);
      			BLMotor.set(-0.25);
    		} else {
      			GearShift.set(DoubleSolenoid.Value.kReverse);
      			FRMotor.set(0);
    	  		BRMotor.set(0);
	      		FLMotor.set(0);
      			BLMotor.set(0);
			}
		}
		if (SelectedAction == "2") {
			GearShift.set(DoubleSolenoid.Value.kReverse);
			FRMotor.set(0);
			BRMotor.set(0);
			FLMotor.set(0);
			BLMotor.set(0);
		}
	
	}

  // The teleop section
  	public void teleopInit() {
		autoTimer.stop();
		autoTimer.reset();
  	}

	public void teleopPeriodic() {
		//gyro
		X = ahrs.getRoll();
		Y = ahrs.getPitch();
		Z = ahrs.getYaw();

		//get joystick values and buttons and such
		RightVal = joyR.getY();
		LeftVal = joyL.getY();
		ExtraVal = joyE.getY();
		groundCollection = joyE.getRawButton(2);
		ballShooter = joyE.getRawButton(1);
		eject = joyE.getRawButton(5);
		winchForwards = joyE.getRawButton(11);
		winchReverse = joyE.getRawButton(12);
		POVhook = joyE.getPOV();
		stationCollection = joyE.getRawButton(3);
		colorRotations = joyE.getRawButton(4);
		colorEndgame = joyE.getRawButton(6);
		gearShift = joyR.getRawButton(1);
		resetButton1 = joyE.getRawButton(7);
		resetButton2 = joyE.getRawButton(8);
		POVgearswitch = joyL.getPOV();
		
		//DriveTrain
		FRMotor.set(RightVal);
		BRMotor.set(RightVal);
		FLMotor.set(LeftVal);
		BLMotor.set(LeftVal);

		//Ground Collection
		if(groundCollection) {
			CollectionDoor.set(DoubleSolenoid.Value.kForward);
			SwifferPiston.set(DoubleSolenoid.Value.kForward);
			SwifferMotor.set(.5);
			BeltMotor.set(.5);
			System.out.println("Collecting from ground");
		}

		//Human player station collection
		if(stationCollection) {
			CollectionDoor.set(DoubleSolenoid.Value.kReverse);
			SwifferPiston.set(DoubleSolenoid.Value.kReverse);
			BeltMotor.set(-0.5);
			System.out.println("Collecting from the human player");
		}

		//Ball shooter
		if(ballShooter) {
			CollectionDoor.set(DoubleSolenoid.Value.kReverse);
			SwifferPiston.set(DoubleSolenoid.Value.kReverse);
			BeltMotor.set(1);
			System.out.println("Lobbing the balls from the cannon thingy");
		}

		//Ball eject
		if(eject) {
			CollectionDoor.set(DoubleSolenoid.Value.kForward);
			SwifferPiston.set(DoubleSolenoid.Value.kForward);
			BeltMotor.set(-1);
			SwifferMotor.set(-1);
			System.out.println("Ejecting balls from collector");
		}

		//Turn the motors off if nothing is pressed
		if(!groundCollection && !stationCollection && !ballShooter && !eject) {
			SwifferMotor.set(0);
			BeltMotor.set(0);	
		}

		//Pull pistons in without running any motors (feel free to remap this button to something else if need be)
		if(resetButton2) {
			CollectionDoor.set(DoubleSolenoid.Value.kReverse);
			SwifferPiston.set(DoubleSolenoid.Value.kReverse);
		}

    if(gearShift) {
      GearShift.set(DoubleSolenoid.Value.kForward);
    } else {
      GearShift.set(DoubleSolenoid.Value.kReverse);
    }
	
		//Classic endgame question of "what color do we need to get again???"
		gameData = DriverStation.getInstance().getGameSpecificMessage();
		if(gameData.length() > 0) {
			switch (gameData.charAt(0))
			{
				case 'R': endgameTargetColor = 1;
					break;
				case 'G': endgameTargetColor = 2;
					break;
				case 'B': endgameTargetColor = 3;
					break;
				case 'Y': endgameTargetColor = 4;
					break;
				default: endgameTargetColor = 0;
					break;
			}
		}
		
		//Color Sensor functions
		currentColor = colorSensor.getColor();
		match = m_colorMatcher.matchClosestColor(currentColor);
		if (match.color == kGreenTarget) {
			colorString = "Green";
			color = 2;
			endRotation = true;
		} else if (match.color == kYellowTarget) {
			colorString = "Yellow";
			color = 4;
		} else if (match.color == kBlueTarget) {
			colorString = "Blue";
			color = 3;
		} else if (match.color == kRedTarget) {
			colorString = "Red";
			color = 1;
		} else {
			colorString = "Unknown";
			color = 0;
			fieldColor = 0;
		}
		if(color != 0) fieldColor = (color+2)%4;
		if (fieldColor == 1 && endRotation) {
			halfRotation = halfRotation + 1;
			endRotation = false;
		}	
    	SmartDashboard.putNumber("Confidence", match.confidence);
		SmartDashboard.putString("Detected Color", colorString);		

		//Normal Color wheel functions
		if (halfRotation == 7) {
			allRotationsDone = true;
		}
		if (colorRotations && !allRotationsDone) {
			ColorMotor.set(ControlMode.PercentOutput, ColorMotorVal);
		}
		if (resetButton1) {
			allRotationsDone = false;
			halfRotation = 0;
			endRotation = false;
		}

		//endgame color wheel, Jonathan's design, Erin's execution, and Jacob's incredible clean-up skillz.
		if(colorEndgame && endgameTargetColor!=fieldColor){
			ColorMotor.set(ControlMode.PercentOutput, ColorMotorVal);
		}

		// Manual motor controls
		if(joyE.getRawButton(10)) {
			if(POVgearswitch == -1) {
				ExtendyBoi.set(ExtraVal);
			}
			if(POVgearswitch == 0) {
				FRMotor.set(ExtraVal);
			}
			if(POVgearswitch == 45) {
				FLMotor.set(ExtraVal);
			}
			if(POVgearswitch == 90) {
				BRMotor.set(ExtraVal);
			}
			if(POVgearswitch == 135) {
				BLMotor.set(ExtraVal);
			}
			if(POVgearswitch == 180) {
				SwifferMotor.set(ExtraVal);
			}
			if(POVgearswitch == 225) {
				BeltMotor.set(ExtraVal);
			}
			if(POVgearswitch == 270) {
				WinchMotor.set(ExtraVal);
			}
			if(POVgearswitch == 315) {
				ColorMotor.set(ExtraVal);
			}
		}
	}
}