
package frc.robot;

//Notes section
//the importer was giving me trouble so I manually changed the current language to java and the project year to 2020 in the wpilib_preferences.json
//Seems like we're doing a secondary pnuematic control module, in theory, as long as it shows up on the phoenix tuner, then it can be assigned to any id.
//speaking of id's, I'm going to need to investigate the sparks, as they do not seem to have the device ID's in the code.
//current autotime needs to be adjusted in auto (autoTimer.get appears to actually get deltatime, which is weird but whatevs)
//auto at 50% speed for 2 seconds is about 103 inches
//auto at 30% speed for 2 seconds is about 53 inches


//imports
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.function.Function;
import com.ctre.phoenix.motorcontrol.ControlMode;
//import com.ctre.phoenix.motorcontrol.can.VictorSPX;
import com.ctre.phoenix.motorcontrol.can.WPI_VictorSPX;
import com.kauailabs.navx.frc.AHRS;
import com.revrobotics.ColorMatch;
import com.revrobotics.ColorMatchResult;
import com.revrobotics.ColorSensorV3;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.first.wpilibj.Compressor;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.I2C;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.util.Color;
import edu.wpi.first.wpilibj.SPI;
import edu.wpi.first.wpilibj.Spark;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.DoubleSolenoid.Value;

public class Robot extends TimedRobot {

//_______________Declarations_______________

	//Hardware Declarations
	//note for the future, Victor is a part of the normal wpi, and the ctre library features the VictorSPX and the WPI_VictorSPX.
	WPI_VictorSPX FRMotor = new WPI_VictorSPX(5);
	WPI_VictorSPX BRMotor = new WPI_VictorSPX(6);
	WPI_VictorSPX FLMotor = new WPI_VictorSPX(7);
	WPI_VictorSPX BLMotor = new WPI_VictorSPX(8);
	WPI_VictorSPX Winch = new WPI_VictorSPX(1);
	WPI_VictorSPX SwifferMotor = new WPI_VictorSPX(4);
	WPI_VictorSPX BeltMotor = new WPI_VictorSPX(3);
	//unknown motor may end up as a secondary climb motor, but this is unlikely
	WPI_VictorSPX UnknownMotor = new WPI_VictorSPX(2);
	Spark Hook1 = new Spark(9);
	Spark ColorMotor = new Spark(10);
	DoubleSolenoid SwifferPiston = new DoubleSolenoid(4, 5);
	DoubleSolenoid GearShift = new DoubleSolenoid(6, 7);
	DoubleSolenoid CollectionDoor = new DoubleSolenoid(2, 3);
	DoubleSolenoid AngleAdjustment = new DoubleSolenoid(0, 1);
	//DoubleSolenoid ColorWheelPiston = new DoubleSolenoid(11, 0, 1);
	Compressor compressor;
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
	boolean winchControl;
	boolean HookControl;
	int POVhook;
	boolean stationCollection;
	boolean colorRotations;
	boolean colorEndgame;
	boolean gearShift;
	boolean resetButton1;
	boolean resetButton2;
	boolean pulseSwiffer;
	int POVMotorSwitch;
	boolean SwifferPistonButton;
	boolean CollectionPistonButton;
	boolean AngleAdjustmentButton;
	boolean directionalShift;
	boolean directionalShiftToggle;
	boolean gearShiftToggle;

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
	double endgameClimbAngle;
	SendableChooser Position = new SendableChooser<>();
	SendableChooser Action = new SendableChooser<>();
	boolean AutoDiverge = false;
	String SelectedMotor = "nothing";
	String SelectedAction;
	String SelectedPosition;
	double currentAutoTime = 0;
	double totalAutoTime = 0;
	double deltaTime = 0;
	double rawGyroVal = 0;
	double processedGyroVal = 0;
	boolean Rejoined = false;
	boolean hasDoorFullyOpened = false;
	boolean isSwifferPulsing = false;


	// This function is called once at the beginning during operator control
	public void robotInit() {
		CameraServer.getInstance().startAutomaticCapture();

		try {
            ahrs = new AHRS(SPI.Port.kMXP); 
        } catch (RuntimeException ex ) {
            DriverStation.reportError("Error instantiating navX MXP:  " + ex.getMessage(), true);
		}
		m_colorMatcher.addColorMatch(kBlueTarget);
		m_colorMatcher.addColorMatch(kGreenTarget);
		m_colorMatcher.addColorMatch(kRedTarget);
		m_colorMatcher.addColorMatch(kYellowTarget);

		//Selected the auto options
		Position.addOption("Left", 1);
		Position.addOption("Middle", 2);
		Position.addOption("Right", 3);
		Position.setDefaultOption("Left", 1);
		SmartDashboard.putData("Starting Position", Position);
		Action.addOption("Move during auto (5pts)", 1);
		Action.addOption("Do nothing (0pts)", 2);
		Action.addOption("Score (up to 11pts)", 3);
		Action.addOption("Boogie (0pts)", 4);
		Action.addOption("Reverse to the Wall (5pts)", 5);
		Action.addOption("Debugging 50% speed for 3 seconds", 6);
		Action.addOption("Debugging 90 degree-ish turn over 1.5 seconds", 7);
		Action.addOption("Debugging 90 degree turn but I use getPitch()", 8);
		Action.addOption("Debugging 90 degree turn but I use processedGyroVal", 9);
		Action.setDefaultOption("Move during auto (5pts)", 1);
		SmartDashboard.putData("Where we droppin' bois", Action);
	}

	//public void robotPeriodic() {
		//I find it unlikely that I'll be using this space, but ya' never know
	//}

  	//Autonomous starts here, beware all ye who dare to look
  	public void autonomousInit() {
		autoTimer.reset();
		autoTimer.start();
	}

  	public void autonomousPeriodic() {
		//lil' bit of setup code before the main event
		SelectedAction = Action.getSelected().toString();
		SelectedPosition = Position.getSelected().toString();
		//currentAutoTime = autoTimer.get();
		//GearShift.set(DoubleSolenoid.Value.kForward);
		//rawGyroVal = ahrs.getRawGyroY();
		
		//Move Forward a bit
		if (SelectedAction.equals("1")) {
   		 	if (currentAutoTime<2) {
				//Drive Forward
				FRMotor.set(-0.3);
				BRMotor.set(-0.3);
				FLMotor.set(0.3);
				BLMotor.set(0.3);
    		} else {
				//Stop Driving forward and revert to default gearshift setting
      			//GearShift.set(DoubleSolenoid.Value.kReverse);
      			FRMotor.set(0);
    	  		BRMotor.set(0);
	      		FLMotor.set(0);
      			BLMotor.set(0);
			}
		}
		//do nothing
		if (SelectedAction == "2") {
			//literally don't do anything at all
			//GearShift.set(DoubleSolenoid.Value.kReverse);
			FRMotor.set(0);
			BRMotor.set(0);
			FLMotor.set(0);
			BLMotor.set(0);
		}
		//this is da big'un, da scorin'
		if (SelectedAction == "3") {
			//Left Position
			if (SelectedPosition == "1" && Rejoined == false) {
				if (currentAutoTime<1.5) {
					//turn 180 degrees to face the opposite direction
					FRMotor.set(0.5);
					BRMotor.set(0.5);
					FLMotor.set(-0.5);
					BLMotor.set(-0.5);
				} 
				if (currentAutoTime>1.5) {
					//hand it over to method 0
					AutoDiverge = true;
					Rejoined = true;
				}
			}
			//Middle or Right Positions (both start by doing the same thing)
			if (SelectedPosition == "2" || SelectedPosition == "3" && Rejoined == false) {
				if (currentAutoTime<1) {
					//move forward a bit
					FRMotor.set(0.5);
					BRMotor.set(0.5);
					FLMotor.set(0.5);
					BLMotor.set(0.5);
				}
				if (currentAutoTime>1.5 && currentAutoTime<3) {
					//turn 90 degrees to the counterclockwise
					FRMotor.set(0.5);
					BRMotor.set(0.5);
					FLMotor.set(-0.5);
					BLMotor.set(-0.5);
				} else {
					//hand it over to the other two methods
					AutoDiverge = true;
				}
			}
			//Middle Position after driving forward and turning
			if (SelectedPosition == "2" && AutoDiverge && Rejoined == false) {
				if (currentAutoTime>3.5 && currentAutoTime<5.5) {
					//drive forward
					FRMotor.set(0.5);
					BRMotor.set(0.5);
					FLMotor.set(0.5);
					BLMotor.set(0.5);
				}
				if (currentAutoTime>6 && currentAutoTime<7.5) {
					//turn 90 degrees to the counterclockwise
					FRMotor.set(0.5);
					BRMotor.set(0.5);
					FLMotor.set(-0.5);
					BLMotor.set(-0.5);
				}
				if (currentAutoTime>7.5) {
					//hand it over to method 0
					Rejoined = true;
				}
			}
			//Left Position after driving forward and turning
			if (SelectedPosition == "3" && AutoDiverge && Rejoined == false) {
				if (currentAutoTime>3.5 && currentAutoTime<6.5) {
					//drive forward
					FRMotor.set(0.5);
					BRMotor.set(0.5);
					FLMotor.set(0.5);
					BLMotor.set(0.5);
				}
				if (currentAutoTime>7 && currentAutoTime<8.5) {
					//turn 90 degrees to the counterclockwise
					FRMotor.set(0.5);
					BRMotor.set(0.5);
					FLMotor.set(-0.5);
					BLMotor.set(-0.5);
				}
				if (currentAutoTime>8.5) {
					//hand it over to method 0
					Rejoined = true;
				}
			}
			//Rejoins all previous parts of the program into one piece
			if (Rejoined == true) {
				//reset timer
				if (AutoDiverge == true) {
					currentAutoTime = 0;
					AutoDiverge = false;
				}
				if (currentAutoTime<2.5) {
					//drive forward to the wall
					FRMotor.set(0.25);
					BRMotor.set(0.25);
					FLMotor.set(0.25);
					BLMotor.set(0.25);
				}
				if (currentAutoTime>3 && currentAutoTime<6) {
					//assume that we're about against the wall and fire the cannon
					FRMotor.set(0);
					BRMotor.set(0);
					FLMotor.set(0);
					BLMotor.set(0);
					BeltMotor.set(1);					
				}
				if (currentAutoTime>6.5) {
					//back up away from the wall
					FRMotor.set(-0.25);
					BRMotor.set(-0.25);
					FLMotor.set(-0.25);
					BLMotor.set(-0.25);
				}
			}
		}
		//Boogie
		if (SelectedAction == "4") {
			if (SelectedPosition == "1") {
				FRMotor.set(1);
				BRMotor.set(1);
				FLMotor.set(-1);
				BLMotor.set(-1);
			}
			if (SelectedPosition == "3") {
				FRMotor.set(-1);
				BRMotor.set(-1);
				FLMotor.set(1);
				BLMotor.set(1);
			}
			if (SelectedPosition == "2") {
				if (currentAutoTime < 8) {
					FRMotor.set(1);
					BRMotor.set(1);
					FLMotor.set(-1);
					BLMotor.set(-1);
				} else {
					FRMotor.set(-1);
					BRMotor.set(-1);
					FLMotor.set(1);
					BLMotor.set(1);
				}
			}
		}
		//back up to the wall-ish
		if (SelectedAction == "5") {
			if (currentAutoTime < 4)
			FRMotor.set(-0.25);
			BRMotor.set(-0.25);
			FLMotor.set(-0.25);
			BLMotor.set(-0.25);
		}
		//move at 50% speed for 3 seconds
		if (SelectedAction == "6") {
			if (currentAutoTime < 3) {
				FRMotor.set(0.5);
				BRMotor.set(0.5);
				FLMotor.set(0.5);
				BLMotor.set(0.5);
			}
		}
		//turn using timing
		if (SelectedAction == "7") {
			if (currentAutoTime < 1.5) {
				//main goal is to get the proper percentage, time adjustments should be made if necessary
				FRMotor.set(-0.5);
				BRMotor.set(-0.5);
				FLMotor.set(0.5);
				BLMotor.set(0.5);
			}
		}
		//turn using ahrs.getPitch()
		if (SelectedAction == "8") {
			if (currentAutoTime < 1.5) {
				//I'm assuming pitch is the correct one, if not then try roll, definitely not yaw
				//two degrees of difference shouldn't be anything crazy at all
				//the direction of the motors may be inverted, in which case, it'll just spin around a bunch
				//also, I don't know if the default value is zero or not, if not, I'm going to have to make a a separate thing to calculate it so that I can zero it out
				if (ahrs.getPitch() > 92) {
					FRMotor.set(-0.5);
					BRMotor.set(-0.5);
					FLMotor.set(0.5);
					BLMotor.set(0.5);
				}
				if (ahrs.getPitch() < 88) {
					FRMotor.set(-0.5);
					BRMotor.set(-0.5);
					FLMotor.set(0.5);
					BLMotor.set(0.5);
				}
			}
		}
		//turn using processedGyroVal
		if (SelectedAction == "9") {
			if (currentAutoTime < 1.5) {
				if (processedGyroVal > 92) {
					FRMotor.set(-0.5);
					BRMotor.set(-0.5);
					FLMotor.set(0.5);
					BLMotor.set(0.5);
				}
				if (processedGyroVal < 88) {
					FRMotor.set(-0.5);
					BRMotor.set(-0.5);
					FLMotor.set(0.5);
					BLMotor.set(0.5);
				}
			}
		}
		//deltaTime stuff (MUST BE AT BOTTOM OF AUTO)
		currentAutoTime = autoTimer.get();
		SmartDashboard.putNumber("deltaTime", deltaTime);
		System.out.println(deltaTime);
		processedGyroVal = rawGyroVal * deltaTime;
		SmartDashboard.putNumber("AUTOTImer", currentAutoTime);
		//currentAutoTime = deltaTime + currentAutoTime;
	}

  // The teleop section
  	public void teleopInit() {
		autoTimer.stop();
		hasDoorFullyOpened = false;
		isSwifferPulsing = false;
		FRMotor.set(0);
		BRMotor.set(0);
		FLMotor.set(0);
		BLMotor.set(0);
		BeltMotor.set(0);
		SwifferMotor.set(0);
		ColorMotor.set(0);
		Winch.set(0);
		Hook1.set(0);
		CollectionDoor.set(DoubleSolenoid.Value.kForward);
		SwifferPiston.set(DoubleSolenoid.Value.kReverse);
		AngleAdjustment.set(DoubleSolenoid.Value.kReverse);
		GearShift.set(DoubleSolenoid.Value.kReverse);
		//ColorWheelPiston.set(DoubleSolenoid.Value.kReverse);
		directionalShiftToggle = false;
		gearShiftToggle = false;
	  }

	public void teleopPeriodic() {
		//get joystick values and buttons and such
		currentAutoTime = autoTimer.get() + currentAutoTime;
		directionalShift = joyL.getRawButtonPressed(1);
		RightVal = joyR.getY();
		LeftVal = joyL.getY();
		ExtraVal = joyE.getY();
		groundCollection = joyE.getRawButton(2);
		ballShooter = joyE.getRawButton(1);
		eject = joyE.getRawButton(5);
		winchControl = joyE.getRawButton(11);
		HookControl = joyE.getRawButton(12);
		POVhook = joyE.getPOV();
		stationCollection = joyE.getRawButton(3);
		colorRotations = joyE.getRawButton(4);
		colorEndgame = joyE.getRawButton(6);
		gearShift = joyR.getRawButtonPressed(1);
		resetButton1 = joyE.getRawButton(7);
		resetButton2 = joyE.getRawButton(8);
		CollectionPistonButton = joyE.getRawButton(10);
		POVMotorSwitch = joyL.getPOV();
		pulseSwiffer = joyE.getRawButton(9);

		if (directionalShift) {
			directionalShiftToggle = !directionalShiftToggle;
		}
		SmartDashboard.putBoolean("Directional Shift Toggle", directionalShiftToggle);
		SmartDashboard.putBoolean("Gear Shift", gearShiftToggle);

		//DriveTrain
		if (!directionalShiftToggle) {
			FRMotor.set(LeftVal);
			BRMotor.set(LeftVal);
			FLMotor.set(-RightVal);
			BLMotor.set(-RightVal);
		}
		if (directionalShiftToggle) {
			FRMotor.set(-RightVal);
			BRMotor.set(-RightVal);
			FLMotor.set(LeftVal);
			BLMotor.set(LeftVal);
		}

		//Climbing
		if (POVhook == 0 && winchControl) {
			Winch.set(1);
		}
		if (POVhook == 180 && winchControl) {
			Winch.set(-0.5);
		}

		if (POVhook == 0 && HookControl) {
			Hook1.set(1);
		}
		if (POVhook == 180 && HookControl) {
			Hook1.set(-1);
		}
		if(!winchControl && !HookControl) {
			Winch.set(0);
			Hook1.set(0);
		}

		//Ground Collection
		if(groundCollection) {
			CollectionDoor.set(DoubleSolenoid.Value.kForward);
			//if (!isSwifferPulsing) {
				SwifferPiston.set(DoubleSolenoid.Value.kForward);
			//}
			AngleAdjustment.set(DoubleSolenoid.Value.kReverse);
			SwifferMotor.set(-0.4);
			BeltMotor.set(0.45);
			System.out.println("Collecting from ground");
			hasDoorFullyOpened = false;
		}

		if (CollectionPistonButton) {
			CollectionDoor.set(DoubleSolenoid.Value.kForward);
			hasDoorFullyOpened = false;
		}

		//Human player station collection
		if(stationCollection) {
			if (!CollectionPistonButton) {
				CollectionDoor.set(DoubleSolenoid.Value.kReverse);
			}
			SwifferPiston.set(DoubleSolenoid.Value.kReverse);
			AngleAdjustment.set(DoubleSolenoid.Value.kForward);
			BeltMotor.set(-0.50);
			SwifferMotor.set(-0.7);
			System.out.println("Collecting from the human player station");
		}

		SmartDashboard.putBoolean("hasdoorfullyopened", hasDoorFullyOpened);
		SmartDashboard.putNumber("autoTimer", currentAutoTime);

		//Ball shooter
		if (ballShooter && !hasDoorFullyOpened) {
			if (!CollectionPistonButton) {
				CollectionDoor.set(DoubleSolenoid.Value.kReverse);
			}
			AngleAdjustment.set(DoubleSolenoid.Value.kForward);
			autoTimer.start();
			if (currentAutoTime > 0.5) {
				hasDoorFullyOpened = true;
			}
		}
		if(ballShooter && hasDoorFullyOpened) {
			SwifferPiston.set(DoubleSolenoid.Value.kReverse);
			BeltMotor.set(1);
			SwifferMotor.set(-.5);
			autoTimer.reset();
			autoTimer.stop();
			currentAutoTime = 0;
		}
		if (!ballShooter && !isSwifferPulsing && currentAutoTime > 0) {
			autoTimer.stop();
			autoTimer.reset();
			currentAutoTime = 0;
		}

		//Ball eject
		if(eject) {
			CollectionDoor.set(DoubleSolenoid.Value.kForward);
			SwifferPiston.set(DoubleSolenoid.Value.kForward);
			AngleAdjustment.set(DoubleSolenoid.Value.kReverse);
			BeltMotor.set(-1);
			SwifferMotor.set(1);
			hasDoorFullyOpened = false;
		}

		//pulse the darn thingy
		//investigate this, by all means, it shouldn't work at all
		if (pulseSwiffer) {
			autoTimer.start();
			if (currentAutoTime < 0.75) {
				SwifferPiston.set(DoubleSolenoid.Value.kReverse);
				isSwifferPulsing = true;
			}
			if (currentAutoTime > 0.75) {
				isSwifferPulsing = false;
				autoTimer.stop();
				//autoTimer.reset();
			}
		}

		//Turn the motors off if nothing is pressed
		if(!groundCollection && !stationCollection && !ballShooter && !eject) {
			SwifferMotor.set(0);
			BeltMotor.set(0);	
		}
		
		//Reset the color wheel values
		if (resetButton1) {
			allRotationsDone = false;
			halfRotation = 0;
			endRotation = false;
			System.out.println("Color wheel stuff reset to default values");
		}

		//Sets robot to smallest configuration
		if(resetButton2 || joyL.getRawButton(10)) {
			System.out.println("Pistons reset to default (in)");
			CollectionDoor.set(DoubleSolenoid.Value.kForward);
			SwifferPiston.set(DoubleSolenoid.Value.kReverse);
			AngleAdjustment.set(DoubleSolenoid.Value.kReverse);
			GearShift.set(DoubleSolenoid.Value.kReverse);
			hasDoorFullyOpened = false;
		}

		//temp buttons for debuggin'
		/*if (AngleAdjustmentButton) {
			AngleAdjustment.set(DoubleSolenoid.Value.kForward);
		}
		if (SwifferPistonButton) {
			SwifferPiston.set(DoubleSolenoid.Value.kForward);
		}
		if (CollectionPistonButton) {
			CollectionDoor.set(DoubleSolenoid.Value.kForward);
		}
		if (joyL.getRawButton(11)) {
			GearShift.set(DoubleSolenoid.Value.kForward);
		}*/

		//shift the gears
		if (gearShift) {
			gearShiftToggle = !gearShiftToggle;
		}
    	if(gearShiftToggle) {
      		GearShift.set(DoubleSolenoid.Value.kForward);
		}
		if(!gearShiftToggle) {
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
		if (colorRotations || colorEndgame) {
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
		}

		if(color != 0) fieldColor = (color+2)%4;
		if (fieldColor == 1 && endRotation) {
			halfRotation = halfRotation + 1;
			endRotation = false;
		}	
    	//SmartDashboard.putNumber("Confidence", match.confidence);
		//SmartDashboard.putString("Detected Color", colorString);		

		//Normal Color wheel functions
		if (halfRotation == 7) {
			allRotationsDone = true;
		}
		if (colorRotations && !allRotationsDone) {
			//ColorWheelPiston.set(DoubleSolenoid.Value.kForward);
			ColorMotor.set(ColorMotorVal);
		}

		//endgame stuffs
		if(colorEndgame && endgameTargetColor!=fieldColor){
			ColorMotor.set(ColorMotorVal);
			//ColorWheelPiston.set(DoubleSolenoid.Value.kForward);
		}

		//turning the motors off and putting the piston into reverse if buttons are not pressed
		if (!colorEndgame && !colorRotations) {
			ColorMotor.set(0);
			//ColorWheelPiston.set(DoubleSolenoid.Value.kReverse);
		}
	}
}