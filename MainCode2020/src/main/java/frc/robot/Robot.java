
package frc.robot;

//Notes section
//the importer was giving me trouble so I manually changed the current language to java and the project year to 2020 in the wpilib_preferences.json
//auto at 50% speed for 2 seconds is about 103 inches
//auto at 30% speed for 2 seconds is about 53 inches
//alliance station wall is 10ft away from the starting line (120 inches)
//maybe don't back up from wall(M0), unless other team members are also doing the low goal and are delaying their stuff such that we don't run into each other, I don't see a point.
//have the door face the alliance wall for all auto methods
//current auto scoring estimations assume that we are going to be starting with our back bumper just barely on the line, which I don't think would count properly, but I haven't---
//---cont. worked out exactly where we'll be starting
//all the sparks seem to work fine
//examine the possibility of running the auto code with some extra smartdashboard data integrated to be certain that everything is progressing how I intend
//auto is configured in such a way that I could disable some functions at the top of auto periodic and come away with a method of doing auto without running anything at all
//forget the whole rejoined thingy, just have 3 different bits of code that all do the same thing so that I don't have to deal with the whole rejoined

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
	WPI_VictorSPX UnknownMotor = new WPI_VictorSPX(2);
	DoubleSolenoid SwifferPiston = new DoubleSolenoid(5, 4);
	DoubleSolenoid GearShift = new DoubleSolenoid(6, 7);
	DoubleSolenoid CollectionDoor = new DoubleSolenoid(2, 3);
	DoubleSolenoid AngleAdjustment = new DoubleSolenoid(0, 1);
	DoubleSolenoid ColorPiston = new DoubleSolenoid(9, 0, 1);
	Spark Hook1 = new Spark(0);
	Spark ColorMotor = new Spark(1);
	Compressor compressor;

	//Joystick declarations
	Joystick joyE = new Joystick(0);
	Joystick joyL = new Joystick(1);
	Joystick joyR = new Joystick(2);
	boolean joyETRigger;
	double RightVal;
	double LeftVal;
	double ExtraVal;
	double AutoBeltVal;
	boolean groundCollection;
	boolean ballShooter;
	boolean eject;
	boolean winchControl;
	boolean HookControl;
	int POVhook;
	boolean stationCollection;
	boolean colorRotations1;
	boolean colorRotations2;
	boolean gearShift;
	boolean resetButton1;
	boolean resetButton2;
	boolean pulseSwiffer;
	boolean SwifferPistonButton;
	boolean CollectionPistonButton;
	boolean AngleAdjustmentButton;
	boolean directionalShift;
	boolean directionalShiftToggle;
	boolean gearShiftToggle;

	//Additional Values
	double ColorMotorVal;
	int endgameTargetColor;
	String gameData;
	Timer autoTimer = new Timer();
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
	boolean A50percentSpeed = false;
	boolean AutoPistonPosition;
	double CATsubtractionAmount = 0;


	// This function is called once at the beginning during operator control
	public void robotInit() {
		CameraServer.getInstance().startAutomaticCapture();

		//Selected the auto options
		Position.addOption("Left", 1);
		Position.addOption("Middle", 2);
		Position.addOption("Right", 3);
		Position.setDefaultOption("Left", 1);
		SmartDashboard.putData("Starting Position", Position);
		Action.addOption("Move during auto (5pts)", 1);
		Action.addOption("Do nothing (0pts)", 2);
		Action.addOption("Score (up to 11pts)", 3);
		Action.addOption("Debugging 90 degree-ish turn over 1.5 seconds", 4);
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
		CollectionDoor.set(DoubleSolenoid.Value.kForward);
		AngleAdjustment.set(DoubleSolenoid.Value.kReverse);
		SwifferPiston.set(DoubleSolenoid.Value.kReverse);
		ColorPiston.set(DoubleSolenoid.Value.kReverse);
		RightVal = 0;
		LeftVal = 0;
		AutoBeltVal = 0;
		AutoPistonPosition = false;
		Rejoined = false;
		CATsubtractionAmount = 0;
	}

  	public void autonomousPeriodic() {
		//FRMotor.set(-RightVal);
		//BRMotor.set(-RightVal);
		//FLMotor.set(LeftVal);
		//BLMotor.set(LeftVal);
		//BeltMotor.set(AutoBeltVal);
		//lil' bit of setup code before the main event
		SelectedAction = Action.getSelected().toString();
		SelectedPosition = Position.getSelected().toString();
		currentAutoTime = autoTimer.get() - CATsubtractionAmount;
		SmartDashboard.putNumber("Current Auto Time", currentAutoTime);
		SmartDashboard.putNumber("RightVal", RightVal);
		SmartDashboard.putNumber("LeftVal", LeftVal);
		SmartDashboard.putNumber("AutoBeltVal", AutoBeltVal);
		SmartDashboard.putBoolean("piston thingy", AutoPistonPosition);

		//back the robot up at 50% speed if true
		if (A50percentSpeed) {
			RightVal = -0.5;
			LeftVal = -0.5;
		}

		//Put the pistons in the right position if true
		/*if (AutoPistonPosition) {
			CollectionDoor.set(DoubleSolenoid.Value.kReverse);
			AngleAdjustment.set(DoubleSolenoid.Value.kForward);
		} else {
			CollectionDoor.set(DoubleSolenoid.Value.kForward);
			AngleAdjustment.set(DoubleSolenoid.Value.kReverse);
		} */

		//the effect of resetting the timer without actually having to reset it at all (thus probably causing problems)
		if (SelectedPosition.equals("1") && Rejoined) {
			CATsubtractionAmount = 0;
		}
		if (SelectedPosition.equals("2") && Rejoined) {
			CATsubtractionAmount = 7.5;
		}
		if (SelectedPosition.equals("3") && Rejoined) {
			CATsubtractionAmount = 8;
		}
		
		//|-------------------------------------------------|
		//|---------- EXITING THE AUTO SETUP AREA ----------|
		//|----------     PRIMARY CODE AHEAD	  ----------|
		//|-------------------------------------------------|

		//Move Forward a bit
		if (SelectedAction.equals("1")) {
   		 	if (currentAutoTime<1.5) {
				//Drive Forward
				RightVal = 0.3;
				LeftVal = 0.3;
    		} else {
				//Stop Driving forward and revert to default gearshift setting
      			//GearShift.set(DoubleSolenoid.Value.kReverse);
				  RightVal = 0;
				  LeftVal = 0;
			}
		}

		//do nothing
		if (SelectedAction.equals("2")) {
			//literally don't do anything at all
			//GearShift.set(DoubleSolenoid.Value.kReverse);
			RightVal = 0;
			LeftVal = 0;
		}

		//this is da big'un, da scorin'
		if (SelectedAction.equals("3")) {
			//Left Position
			if (SelectedPosition.equals("1")) {
				Rejoined = true;
			}

			//Middle or Right Positions (both start by doing the same thing)
			if (SelectedPosition.equals("2") || SelectedPosition.equals("3") && !Rejoined && !AutoDiverge){
				if (currentAutoTime<1.5) {
					//back up a bit
					//goal is 50-ish inches
					A50percentSpeed = true;
				}
				if (currentAutoTime>1.5 && currentAutoTime<3) {
					//turn 90 degrees to the counterclockwise
					A50percentSpeed = false;
					RightVal = 0.5;
					LeftVal = -0.5;
				} else {
					//hand it over to the other two methods
					AutoDiverge = true;
				}
			}

			//Middle Position after driving forward and turning
			if (SelectedPosition.equals("2") && AutoDiverge && !Rejoined) {
				if (currentAutoTime>3.5 && currentAutoTime<5) {
					//drive forward
					//goal is 60 (ish) inches
					A50percentSpeed = true;
				}
				if (currentAutoTime>6 && currentAutoTime<7.5) {
					//turn 90 degrees clockwise
					A50percentSpeed = false;
					RightVal = -0.5;
					LeftVal = 0.5;
				}
				if (currentAutoTime>7.5) {
					//hand it over to method 0
					Rejoined = true;
				}
			}

			//Left Position after driving forward and turning
			if (SelectedPosition.equals("3") && AutoDiverge && !Rejoined) {
				if (currentAutoTime>3 && currentAutoTime<5.5) {
					//drive forward
					//goal is just under 130 inches (ish)
					A50percentSpeed = true;
				}
				if (currentAutoTime>6 && currentAutoTime<7.5) {
					//turn 90 degrees counterclockwise
					A50percentSpeed = false;
					RightVal = 0.5;
					LeftVal = -0.5;
				}
				if (currentAutoTime>8) {
					//hand it over to method 0
					Rejoined = true;
				}
			}
			//Rejoins all previous parts of the program into one piece
			if (Rejoined == true || SelectedPosition.equals("1")) {
				//drive forward to the wall
				//goal is 70 inches
				//changed from 1.5 seconds to just 1 second to account for the fact that we're not going to start on the edge of the line but rather about the middle
				if (currentAutoTime<1) {
					A50percentSpeed = true;
				}
				//extra movement if we start on the left side
				//goal is 50 inches
				if (currentAutoTime>1 && currentAutoTime<2 && SelectedPosition.equals("1")) {
					A50percentSpeed = false;
					RightVal = -0.4;
					LeftVal = -0.4;
				}
				//actuate pistons
				if (currentAutoTime>1 && currentAutoTime<3) {
					AutoPistonPosition = true;
					if (!SelectedPosition.equals("1")) {
						A50percentSpeed = false;
						RightVal = 0;
						LeftVal = 0;
					}
					if (SelectedPosition.equals("1") && currentAutoTime > 2) {
						RightVal = 0;
						LeftVal = 0;
					}
				}
				//assume that we're about against the wall and fire the cannon
				if (currentAutoTime>3 && currentAutoTime<4.5) {
					AutoBeltVal = 1;
				}
				//back up away from the wall
				if (currentAutoTime>4.5 && currentAutoTime<6) {
					AutoPistonPosition = false;
					AutoBeltVal = 0;
					RightVal = 0.3;
					LeftVal = 0.3;
				}
			}
		}

		//turn using timing
		if (SelectedAction.equals("4")) {
			if (currentAutoTime < 1.5) {
				//main goal is to get the proper percentage, time adjustments should be made if necessary
				RightVal = -0.5;
				LeftVal = 0.5;
			}
		}
	}

	//The teleop section
  	public void teleopInit() {
		autoTimer.stop();
		hasDoorFullyOpened = false;
		isSwifferPulsing = false;
		RightVal = 0;
		LeftVal = 0;
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
		ColorPiston.set(DoubleSolenoid.Value.kReverse);
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
		colorRotations1 = joyE.getRawButton(4);
		colorRotations2 = joyE.getRawButton(6);
		gearShift = joyR.getRawButtonPressed(1);
		resetButton1 = joyE.getRawButton(7);
		resetButton2 = joyE.getRawButton(8);
		CollectionPistonButton = joyE.getRawButton(10);
		pulseSwiffer = joyE.getRawButton(9);

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
		if (directionalShift) {
			directionalShiftToggle = !directionalShiftToggle;
		}

		//Climbing
		if (winchControl) {
			Winch.set(ExtraVal);
		}
		if (HookControl) {
			Hook1.set(ExtraVal);
		}
		if(!winchControl && !HookControl) {
			Winch.set(0);
			Hook1.set(0);
		}

		//Ground Collection
		if(groundCollection) {
			CollectionDoor.set(DoubleSolenoid.Value.kForward);
			if (!isSwifferPulsing) {
				SwifferPiston.set(DoubleSolenoid.Value.kForward);
			}
			AngleAdjustment.set(DoubleSolenoid.Value.kReverse);
			SwifferMotor.set(-0.4);
			BeltMotor.set(0.45);
			System.out.println("Collecting from ground");
			hasDoorFullyOpened = false;
		}
		if (pulseSwiffer) {
			SwifferPiston.set(DoubleSolenoid.Value.kReverse);
			isSwifferPulsing = true;
		} else {
			isSwifferPulsing = false;
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

		//Ball shooter
		if (ballShooter && !hasDoorFullyOpened) {
			if (!CollectionPistonButton) {
				CollectionDoor.set(DoubleSolenoid.Value.kReverse);
			}
			AngleAdjustment.set(DoubleSolenoid.Value.kForward);
			SwifferPiston.set(DoubleSolenoid.Value.kReverse);
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

		//Turn the motors off if nothing is pressed
		if(!groundCollection && !stationCollection && !ballShooter && !eject) {
			SwifferMotor.set(0);
			BeltMotor.set(0);	
		}
		
		//Reset the color wheel values
		if (resetButton1) {
			//currently empty, but I'm sure there'll be something
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
	
		//Classic endgame color
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
		
		//Color Motor stuffs
		if (colorRotations2) {
			ColorMotor.set(-0.5);
			ColorPiston.set(DoubleSolenoid.Value.kForward);
		}
		if (colorRotations1) {
			ColorMotor.set(1);
			ColorPiston.set(DoubleSolenoid.Value.kForward);
		}
		if (!colorRotations2 && !colorRotations1) {
			ColorMotor.set(0);
			ColorPiston.set(DoubleSolenoid.Value.kReverse);
		}
		SmartDashboard.putNumber("Target Color", endgameTargetColor);
	}
}