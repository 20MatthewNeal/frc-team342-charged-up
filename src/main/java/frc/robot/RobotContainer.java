// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import frc.robot.Constants.OperatorConstants;
import frc.robot.commands.*;
import frc.robot.subsystems.*;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.util.sendable.Sendable;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandBase;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.button.JoystickButton;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import edu.wpi.first.wpilibj2.command.button.Trigger;

/**
 * This class is where the bulk of the robot should be declared. Since Command-based is a
 * "declarative" paradigm, very little robot logic should actually be handled in the {@link Robot}
 * periodic methods (other than the scheduler calls). Instead, the structure of the robot (including
 * subsystems, commands, and trigger mappings) should be declared here.
 */
public class RobotContainer {
  // The robot's subsystems and commands are defined here...
  private final DriveSystem driveSystem;
  private final Limelight limelight;

  private final GripperSystem gripperSystem;
  private final XboxController driver = new XboxController(OperatorConstants.kDriverControllerPort);
  private final JoystickButton xButton = new JoystickButton(driver, XboxController.Button.kX.value);

  private SendableChooser<Command> autoChooser;

  // hardware connection check stuff
  private final NetworkTable hardware = NetworkTableInstance.getDefault().getTable("Hardware");

  /** The container for the robot. Contains subsystems, OI devices, and commands. */
  public RobotContainer() {
    driveSystem = new DriveSystem();
    driveSystem.setDefaultCommand(driveSystem.driveWithJoystick(driver));
    
    gripperSystem = new GripperSystem();

    limelight = new Limelight();

    SmartDashboard.putData(driveSystem);
    SmartDashboard.putData(gripperSystem);
    SmartDashboard.putData(limelight);
    
    // Configure the trigger bindings
    configureBindings();

    // hardware check
    Shuffleboard.getTab("Hardware").add(getCheckCommand());
    Shuffleboard.getTab("Hardware").add(CommandScheduler.getInstance());

    autoChooser = new SendableChooser<>();
    autoChooser.setDefaultOption("b", Autos.driveUpAndBalance(driveSystem));
    autoChooser.addOption("a", Autos.doNothing(driveSystem));
    autoChooser.addOption("e", Autos.leftSide(driveSystem));

  }

  /**
   * Use this method to define your trigger->command mappings. Triggers can be created via the
   * {@link Trigger#Trigger(java.util.function.BooleanSupplier)} constructor with an arbitrary
   * predicate, or via the named factories in {@link
   * edu.wpi.first.wpilibj2.command.button.CommandGenericHID}'s subclasses for {@link
   * CommandXboxController Xbox}/{@link edu.wpi.first.wpilibj2.command.button.CommandPS4Controller
   * PS4} controllers or {@link edu.wpi.first.wpilibj2.command.button.CommandJoystick Flight
   * joysticks}.
   */
  private void configureBindings() {
    xButton.whileTrue(gripperSystem.intake());
  }

  private CommandBase getCheckCommand() {
    return Commands.sequence(
      // drive
      new InstantCommand(
        () -> { hardware.getEntry("Drive").setString(driveSystem.checkAllConnections()); },
        driveSystem
      ),

      // limelight
      new InstantCommand(
        () -> { hardware.getEntry("Limelight").setString(limelight.checkAllConnections()); }
      )
    ).ignoringDisable(true);
  }

  /**
   * Use this to pass the autonomous command to the main {@link Robot} class.
   *
   * @return the command to run in autonomous
   */
  public Command getAutonomousCommand() {
    return autoChooser.getSelected();
  }

  public Command getTestCommand() {
    // return all test routines chained together
    return new SequentialCommandGroup(
      // check that all devices are connected
      getCheckCommand(),
      
      /*
       * drive system test routine:
       * - drive forwards
       * - drive backwards
       * - turn clockwise
       * - turn counterclockwise
       * - drive forwards in slow mode
       */
      driveSystem.testRoutine()
    );
  }
}
