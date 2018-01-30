/*===============================================================================
Copyright (c) 2016 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.vuforia.samples.SampleApplication.utils;

import android.opengl.GLES20;
import android.util.Log;


public class SampleUtils
{
    
    private static final String LOGTAG = "SampleUtils";
    
    
    static int initShader(int shaderType, String source)
    {
        /*
        glCreateShader(Glenum type) : 쉐이더 객체 생성
        type : 생성하는 쉐이더의 타입으로 vertex shader/fragment shader
        return : 새로운 쉐이더 객체에 대한 핸들.
        */
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0)
        {
            /*
            Glunit shader : 쉐이더 객체 핸들
            string source : 문자열 배열의 포인터
             */
            GLES20.glShaderSource(shader, source);//쉐이더 소스코드 제공
            GLES20.glCompileShader(shader);//shader객체에 저장된 소스코드가 컴파일
            
            int[] glStatusVar = { GLES20.GL_FALSE };
            /*
            shader : 정보를 얻기 위한 쉐이더 핸들
            pname : 정보를 얻기위한 매개변수
            params : 질의 결과에 대한 정수형 기억 장소에 대한 포인터
            offset
             */
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, glStatusVar,
                0);//쉐이더가 성공적으로 컴파일되었는지 확인
            if (glStatusVar[0] == GLES20.GL_FALSE)
            {
                Log.e(LOGTAG, "Could NOT compile shader " + shaderType + " : "
                    + GLES20.glGetShaderInfoLog(shader));//정보로그 검색. 오류메세지 반환
                GLES20.glDeleteShader(shader);//쉐이더더를 즉시 삭제하지 않고, 삭제의 대상으로 표시. 더이상 프로그램 객체에 첨부되지 않으면 메모리 해제
                shader = 0;
            }
            
        }
        
        return shader;
    }
    
    
    public static int createProgramFromShaderSrc(String vertexShaderSrc,
        String fragmentShaderSrc)
    {
        int vertShader = initShader(GLES20.GL_VERTEX_SHADER, vertexShaderSrc);
        int fragShader = initShader(GLES20.GL_FRAGMENT_SHADER,
            fragmentShaderSrc);
        
        if (vertShader == 0 || fragShader == 0)
            return 0;
        //프로그램 객체 생성. 프로그램 객체 : 쉐이더를 첨부하고 최종적인 실행 가능한 프로그램을 링크하는 컨테이너 객체
        int program = GLES20.glCreateProgram();//프로그램 객체에 핸들 반환
        if (program != 0)
        {
            GLES20.glAttachShader(program, vertShader);//프로그램에 쉐이더 첨부
            checkGLError("glAttchShader(vert)");
            
            GLES20.glAttachShader(program, fragShader);
            checkGLError("glAttchShader(frag)");
            
            GLES20.glLinkProgram(program);//프로그램 링크 -> 최종적인 실행 가능한 프로그램 생성
            int[] glStatusVar = { GLES20.GL_FALSE };
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, glStatusVar,//프로그램 연결상태 점검
                0);
            if (glStatusVar[0] == GLES20.GL_FALSE)
            {
                Log.e(
                    LOGTAG,
                    "Could NOT link program : "
                        + GLES20.glGetProgramInfoLog(program));//정보로그
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        
        return program;
    }
    
    
    public static void checkGLError(String op)
    {
        for (int error = GLES20.glGetError(); error != 0; error = GLES20
            .glGetError())
            Log.e(
                LOGTAG,
                "After operation " + op + " got glError 0x"
                    + Integer.toHexString(error));
    }
    
    
    // Transforms a screen pixel to a pixel onto the camera image,
    // taking into account e.g. cropping of camera image to fit different aspect
    // ratio screen.
    // for the camera dimensions, the width is always bigger than the height
    // (always landscape orientation)
    // Top left of screen/camera is origin
    public static void screenCoordToCameraCoord(int screenX, int screenY,
        int screenDX, int screenDY, int screenWidth, int screenHeight,
        int cameraWidth, int cameraHeight, int[] cameraX, int[] cameraY,
        int[] cameraDX, int[] cameraDY, int displayRotation, int cameraRotation)
    {
        float videoWidth, videoHeight;
        videoWidth = (float) cameraWidth;
        videoHeight = (float) cameraHeight;

        // Compute the angle by which the camera image should be rotated clockwise so that it is
        // shown correctly on the display given its current orientation.
        int correctedRotation = ((((displayRotation*90)-cameraRotation)+360)%360)/90;

        switch (correctedRotation) {

            case 0:
                break;

            case 1:

                int tmp = screenX;
                screenX = screenHeight - screenY;
                screenY = tmp;

                tmp = screenDX;
                screenDX = screenDY;
                screenDY = tmp;

                tmp = screenWidth;
                screenWidth = screenHeight;
                screenHeight = tmp;

                break;

            case 2:
                screenX = screenWidth - screenX;
                screenY = screenHeight - screenY;
                break;

            case 3:

                tmp = screenX;
                screenX = screenY;
                screenY = screenWidth - tmp;

                tmp = screenDX;
                screenDX = screenDY;
                screenDY = tmp;

                tmp = screenWidth;
                screenWidth = screenHeight;
                screenHeight = tmp;

                break;
        }
        
        float videoAspectRatio = videoHeight / videoWidth;
        float screenAspectRatio = (float) screenHeight / (float) screenWidth;
        
        float scaledUpX;
        float scaledUpY;
        float scaledUpVideoWidth;
        float scaledUpVideoHeight;
        
        if (videoAspectRatio < screenAspectRatio)
        {
            // the video height will fit in the screen height
            scaledUpVideoWidth = (float) screenHeight / videoAspectRatio;
            scaledUpVideoHeight = screenHeight;
            scaledUpX = (float) screenX
                + ((scaledUpVideoWidth - (float) screenWidth) / 2.0f);
            scaledUpY = (float) screenY;
        } else
        {
            // the video width will fit in the screen width
            scaledUpVideoHeight = (float) screenWidth * videoAspectRatio;
            scaledUpVideoWidth = screenWidth;
            scaledUpY = (float) screenY
                + ((scaledUpVideoHeight - (float) screenHeight) / 2.0f);
            scaledUpX = (float) screenX;
        }
        
        if (cameraX != null && cameraX.length > 0)
        {
            cameraX[0] = (int) ((scaledUpX / (float) scaledUpVideoWidth) * videoWidth);
        }
        
        if (cameraY != null && cameraY.length > 0)
        {
            cameraY[0] = (int) ((scaledUpY / (float) scaledUpVideoHeight) * videoHeight);
        }
        
        if (cameraDX != null && cameraDX.length > 0)
        {
            cameraDX[0] = (int) (((float) screenDX / (float) scaledUpVideoWidth) * videoWidth);
        }
        
        if (cameraDY != null && cameraDY.length > 0)
        {
            cameraDY[0] = (int) (((float) screenDY / (float) scaledUpVideoHeight) * videoHeight);
        }
    }
    
    
    public static float[] getOrthoMatrix(float nLeft, float nRight,
        float nBottom, float nTop, float nNear, float nFar)
    {
        float[] nProjMatrix = new float[16];
        
        int i;
        for (i = 0; i < 16; i++)
            nProjMatrix[i] = 0.0f;
        
        nProjMatrix[0] = 2.0f / (nRight - nLeft);
        nProjMatrix[5] = 2.0f / (nTop - nBottom);
        nProjMatrix[10] = 2.0f / (nNear - nFar);
        nProjMatrix[12] = -(nRight + nLeft) / (nRight - nLeft);
        nProjMatrix[13] = -(nTop + nBottom) / (nTop - nBottom);
        nProjMatrix[14] = (nFar + nNear) / (nFar - nNear);
        nProjMatrix[15] = 1.0f;
        
        return nProjMatrix;
    }
    
}
