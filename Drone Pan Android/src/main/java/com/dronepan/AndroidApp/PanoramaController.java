package com.dronepan.AndroidApp;

import android.util.Log;

import java.util.LinkedList;

import dji.sdk.Gimbal.DJIGimbal;
import dji.sdk.MissionManager.DJICustomMission;
import dji.sdk.MissionManager.DJIMission;
import dji.sdk.MissionManager.DJIMissionManager;
import dji.sdk.MissionManager.MissionStep.DJIGimbalAttitudeStep;
import dji.sdk.MissionManager.MissionStep.DJIGoToStep;
import dji.sdk.MissionManager.MissionStep.DJIMissionStep;
import dji.sdk.MissionManager.MissionStep.DJIShootPhotoStep;
import dji.sdk.MissionManager.MissionStep.DJIAircraftYawStep;
import dji.sdk.MissionManager.MissionStep.DJITakeoffStep;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIError;

public class PanoramaController {
    private static final String TAG = PanoramaController.class.getName();

    interface PanoramaControllerInterface {
        void postUserMessage(String message);
        void postUserWarning(String warning);
        void panoStarting();
        void panoStopping();
        void gimbalAttitudeChanged(float pitch, float yaw, float roll);
        void aircraftYawChanged(float yaw);
        void aircraftSattelitesChanged(int count);
        void aircraftDistanceChanged(float lat, float lng);
        void aircraftAltitudeChanged(float altitude);
        void panoCountChanged(int count, int total);
        void panoAvailable(boolean available);

    }

    public PanoramaControllerInterface delegate = null;

    private MainViewController mContext = null;

    private CameraController mCameraController;
    private DJIMissionManager mDJIMissionManager;
    private DJIMission mDJIMission;

    private float lastGimbalPtich = 0.0f;
    private float lastGimbalYaw = 0.0f;
    private float lastGimbalRoll = 0.0f;
    private float lastACYaw = 0.0f;

    private boolean isRunningState = false;
    private boolean isRunningOK = false;

    public PanoramaController(MainViewController ctx) {
        mContext = ctx;
    }

    // START PANORAMA
    public void start() {

        if (!checkProduct()) {
            return;
        }

        if (!checkCamera()) {
            return;
        }

        if (!checkSpace()) {
            return;
        }

        if (!checkGimbal()) {
            return;
        }

        if (!checkFC()) {
            return;
        }

        if (!checkRemote()) {
            return;
        }

        if (!checkRCMode()) {
            return;
        }

        isRunningState = true;
        isRunningOK = true;

        // PANORAMA STARTING
        doPanoLoop();

    }

    // DO PANO LOOP
    protected void doPanoLoop() {
        int currentCount = 0;

        delegate.postUserMessage("doPanoLoop");

        DJIMissionManager missionManager = DJIMissionManager.getInstance();

        if (missionManager == null) {
            delegate.postUserMessage("Error: Could not get mission manager instance");
            return;
        }

        mDJIMission = createCustomMission();
        if (mDJIMission == null) {
            delegate.postUserMessage("Please choose a mission type");
            //Utils.setResultToToast(mContext, "Please choose a mission type...");
        }
        missionManager.prepareMission(mDJIMission, new DJIMission.DJIMissionProgressHandler() {

            @Override
            public void onProgress(DJIMission.DJIProgressType type, float progress) {
                //setProgressBar((int)(progress * 100f));
            }

        }, new DJIBaseComponent.DJICompletionCallback() {


            @Override
            public void onResult(DJIError error) {
                if (error == null) {
                    //Utils.setResultToToast(mContext, "Success!");

                    delegate.postUserMessage("Success preparing mission");

                    startMission();
                } else {
                    delegate.postUserMessage("Error preparing mission " + error.getDescription());
                }
            }
        });
    }

    protected void startMission() {
        delegate.postUserMessage("Starting mission");

        DJIMissionManager missionManager = DJIMissionManager.getInstance();

        if (mDJIMission != null) {
            missionManager.setMissionExecutionFinishedCallback(new DJIBaseComponent.DJICompletionCallback() {

                @Override
                public void onResult(DJIError error) {

                    if(error == null) {
                        delegate.postUserMessage("Mission executing OK");
                    }

                }
            });

            missionManager.startMissionExecution(new DJIBaseComponent.DJICompletionCallback() {

                @Override
                public void onResult(DJIError mError) {

                    if (mError == null) {
                        delegate.postUserMessage("Mission execution sucess");
                    } else {
                        delegate.postUserMessage("Error mission execution: " + mError.getDescription());
                    }
                }
            });
        }
    }

    protected DJICustomMission createCustomMission() {
        // CREATE STEP LIST
        LinkedList<DJIMissionStep> steps = new LinkedList<DJIMissionStep>();

        // CREATE 6 STEPS
        createRowStep(steps);
        createRowStep(steps);
        createRowStep(steps);
        createRowStep(steps);
        createRowStep(steps);
        createRowStep(steps);

        // SET GIMBAL STATE
        steps.add(new DJIGimbalAttitudeStep(
                DJIGimbal.DJIGimbalRotateAngleMode.AbsoluteAngle,
                new DJIGimbal.DJIGimbalAngleRotation(true, -90f, DJIGimbal.DJIGimbalRotateDirection.Clockwise),
                null,
                null,
                new DJIBaseComponent.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        //delegate.postUserMessage("RESET GIMBAL: " + DJIGimbal.DJIGimbalRotateDirection.Clockwise + " " + (error == null ? "Success" : error.getDescription()));
                    }

                }));

        // SHOOT PHOTO
        steps.add(new DJIShootPhotoStep(new DJIBaseComponent.DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                //Utils.setResultToToast(mContext, "Take single photo step: " + (error == null ? "Success" : error.getDescription()));
                delegate.postUserMessage("PANORAMA SUCCESS");
            }
        }));



        DJICustomMission customMission = new DJICustomMission(steps);

        return customMission;
    }

    protected void createRowStep(LinkedList<DJIMissionStep> steps) {
        float currentGimbalAngle = 0.0f;

        // RESET GIMBAL STATE
        steps.add(new DJIGimbalAttitudeStep(
                DJIGimbal.DJIGimbalRotateAngleMode.AbsoluteAngle,
                new DJIGimbal.DJIGimbalAngleRotation(true, currentGimbalAngle, DJIGimbal.DJIGimbalRotateDirection.Clockwise),
                null,
                null,
                new DJIBaseComponent.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        //delegate.postUserMessage("RESET GIMBAL: " + DJIGimbal.DJIGimbalRotateDirection.Clockwise + " " + (error == null ? "Success" : error.getDescription()));
                    }

                }));

        // SHOOT PHOTO
        steps.add(new DJIShootPhotoStep(new DJIBaseComponent.DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                //Utils.setResultToToast(mContext, "Take single photo step: " + (error == null ? "Success" : error.getDescription()));
                //delegate.postUserMessage("SHOOT PHOTO STEP");
            }
        }));

        currentGimbalAngle -= 30.0f;
        // -30

        // SET GIMBAL STATE
        steps.add(new DJIGimbalAttitudeStep(
                DJIGimbal.DJIGimbalRotateAngleMode.AbsoluteAngle,
                new DJIGimbal.DJIGimbalAngleRotation(true, currentGimbalAngle, DJIGimbal.DJIGimbalRotateDirection.Clockwise),
                null,
                null,
                new DJIBaseComponent.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        //delegate.postUserMessage("RESET GIMBAL: " + DJIGimbal.DJIGimbalRotateDirection.Clockwise + " " + (error == null ? "Success" : error.getDescription()));
                    }

                }));

        // SHOOT PHOTO
        steps.add(new DJIShootPhotoStep(new DJIBaseComponent.DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                //Utils.setResultToToast(mContext, "Take single photo step: " + (error == null ? "Success" : error.getDescription()));
                //delegate.postUserMessage("SHOOT PHOTO STEP");
            }
        }));

        currentGimbalAngle -= 30.0f;
        // -60

        // SET GIMBAL STATE
        steps.add(new DJIGimbalAttitudeStep(
                DJIGimbal.DJIGimbalRotateAngleMode.AbsoluteAngle,
                new DJIGimbal.DJIGimbalAngleRotation(true, currentGimbalAngle, DJIGimbal.DJIGimbalRotateDirection.Clockwise),
                null,
                null,
                new DJIBaseComponent.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        //delegate.postUserMessage("RESET GIMBAL: " + DJIGimbal.DJIGimbalRotateDirection.Clockwise + " " + (error == null ? "Success" : error.getDescription()));
                    }

                }));

        // SHOOT PHOTO
        steps.add(new DJIShootPhotoStep(new DJIBaseComponent.DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                //Utils.setResultToToast(mContext, "Take single photo step: " + (error == null ? "Success" : error.getDescription()));
                delegate.postUserMessage("LAST PANORAMA SHOOT PHOTO STEP");
            }
        }));

        // END ROW

        // YAW AIRCRAFT
        steps.add(new DJIAircraftYawStep(60.0f, 10.0f, new DJIBaseComponent.DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                //Utils.setResultToToast(mContext, "Take single photo step: " + (error == null ? "Success" : error.getDescription()));
                delegate.postUserMessage("AIRCRAFT YAW STEP");
            }
        }));

    }

    public void buildMissionSteps() {

    }

    public boolean checkProduct() {

        return true;
    }

    public boolean checkCamera() {

        return true;
    }

    public boolean checkSpace() {

        return true;
    }

    public boolean checkGimbal() {

        return true;
    }

    public boolean checkFC() {

        return true;
    }

    public boolean checkRemote() {

        return true;
    }

    public boolean checkRCMode() {

        return true;
    }


    // CAPTURE PHOTO
    public void capturePhoto() {

        // SET CAMERA MODE TO SHOOT PHOTO
        //mDJIController.switchCameraMode(DJICameraSettingsDef.CameraMode.ShootPhoto);

        // CAPTURE PHOTO
        //mDJIController.capturePhoto();

    }

    // UPDATE VISUAL DEBUG DATA
    public void updateVisualDebugData() {

        /*if(mDJIController.isAircrafConnected()) {
            mMainActivity.setTitleBar(mDJIController.getAircraftModel() + " connected!");
        }
        else {
            mMainActivity.setTitleBar("No aircraft connected!");
        }*/

    }

}