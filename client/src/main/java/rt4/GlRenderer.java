package rt4;

import com.jogamp.nativewindow.awt.AWTGraphicsConfiguration;
import com.jogamp.nativewindow.awt.JAWTWindow;
import com.jogamp.opengl.*;
import com.jogamp.opengl.GLCapabilities;
import jogamp.newt.awt.NewtFactoryAWT;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.system.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.awt.*;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;

public final class GlRenderer {

	@OriginalMember(owner = "client!tf", name = "a", descriptor = "Ljava/lang/String;")
	private static String vendor;

	@OriginalMember(owner = "client!tf", name = "b", descriptor = "Ljava/lang/String;")
	private static String renderer;
	public static float vFOV = 0;
	public static float hFOV = 0;

	public static int leftMargin;

	public static int topMargin;

	public static int viewportWidth;

	public static int viewportHeight;

	@OriginalMember(owner = "client!tf", name = "c", descriptor = "F")
	private static float depthScaleFactor;

	@OriginalMember(owner = "client!tf", name = "e", descriptor = "I")
	public static int maxTextureUnits;

	@OriginalMember(owner = "client!tf", name = "f", descriptor = "Z")
	public static boolean bigEndian;

	@OriginalMember(owner = "client!tf", name = "k", descriptor = "F")
	private static float scaledFarClipDistance;

	@OriginalMember(owner = "client!tf", name = "p", descriptor = "Lgl!javax/media/opengl/GLContext;")
	private static GLContext context;

	@OriginalMember(owner = "client!tf", name = "r", descriptor = "Z")
	public static boolean extTexture3dSupported;

	@OriginalMember(owner = "client!tf", name = "t", descriptor = "Lgl!javax/media/opengl/GL;")
	public static GL2 gl;

	@OriginalMember(owner = "client!tf", name = "v", descriptor = "I")
	private static int maxTextureImageUnits;

	@OriginalMember(owner = "client!tf", name = "y", descriptor = "Z")
	public static boolean arbMultisampleSupported;

	@OriginalMember(owner = "client!tf", name = "z", descriptor = "I")
	public static int anInt5328;

	@OriginalMember(owner = "client!tf", name = "A", descriptor = "I")
	public static int canvasHeight;

	@OriginalMember(owner = "client!tf", name = "B", descriptor = "I")
	private static int version;

	@OriginalMember(owner = "client!tf", name = "C", descriptor = "Z")
	public static boolean arbVboSupported;

	@OriginalMember(owner = "client!tf", name = "D", descriptor = "I")
	private static int maxTextureCoords;

	private static long LWJGLwindow;

	@OriginalMember(owner = "client!tf", name = "E", descriptor = "Lgl!javax/media/opengl/GLDrawable;")
	private static GLDrawable drawable;

	@OriginalMember(owner = "client!tf", name = "H", descriptor = "Z")
	public static boolean arbVertexProgramSupported;

	@OriginalMember(owner = "client!tf", name = "J", descriptor = "I")
	public static int canvasWidth;

	@OriginalMember(owner = "client!tf", name = "K", descriptor = "Z")
	public static boolean arbTextureCubeMapSupported;

	@OriginalMember(owner = "client!tf", name = "d", descriptor = "Z")
	private static boolean textureMatrixModified = false;

	@OriginalMember(owner = "client!tf", name = "g", descriptor = "I")
	public static int anInt5323 = 0;

	@OriginalMember(owner = "client!tf", name = "h", descriptor = "I")
	private static int textureCombineAlphaMode = 0;

	@OriginalMember(owner = "client!tf", name = "i", descriptor = "I")
	private static int textureCombineRgbMode = 0;

	@OriginalMember(owner = "client!tf", name = "j", descriptor = "F")
	private static float depthAdjustmentFactor = 0.0F;

	@OriginalMember(owner = "client!tf", name = "l", descriptor = "Z")
	private static boolean lightingEnabled = true;

	@OriginalMember(owner = "client!tf", name = "m", descriptor = "F")
	private static float depthAdjustmentParameter = 0.0F;

	@OriginalMember(owner = "client!tf", name = "n", descriptor = "Z")
	public static boolean normalArrayEnabled = true;

	@OriginalMember(owner = "client!tf", name = "o", descriptor = "Z")
	private static boolean isOrthoViewConfigured = false;

	@OriginalMember(owner = "client!tf", name = "q", descriptor = "F")
	private static final float projectionCoordinateScaleFactor = 0.09765625F;

	@OriginalMember(owner = "client!tf", name = "s", descriptor = "I")
	private static int textureId = -1;

	@OriginalMember(owner = "client!tf", name = "u", descriptor = "Z")
	private static boolean depthTestEnabled = true;

	@OriginalMember(owner = "client!tf", name = "w", descriptor = "Z")
	public static boolean enabled = false;

	@OriginalMember(owner = "client!tf", name = "x", descriptor = "[F")
	private static final float[] matrix = new float[16];

	@OriginalMember(owner = "client!tf", name = "F", descriptor = "Z")
	private static boolean fogEnabled = true;

	@OriginalMember(owner = "client!tf", name = "I", descriptor = "Lclient!na;")
	private static final JagString RADEON = JagString.parse("radeon");

	private static JAWTWindow window;

	public static void glDrawElementsWrapper(int mode, int count, int type, java.nio.Buffer buffer) {
		long pointer = MemoryUtil.memAddress(buffer);
		glDrawElements(mode, count, type, pointer);
	}

	@OriginalMember(owner = "client!tf", name = "a", descriptor = "(Ljava/lang/String;)Lclient!na;")
	private static JagString convertStringToJagString(@OriginalArg(0) String s) {
		@Pc(3) byte[] bytes;
		bytes = s.getBytes(StandardCharsets.ISO_8859_1);
		return JagString.decodeString(bytes, bytes.length, 0);
	}

	@OriginalMember(owner = "client!tf", name = "a", descriptor = "(IIII)V")
	public static void method4148(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2, @OriginalArg(3) int arg3) {
		setupViewTransformations(0, 0, canvasWidth, canvasHeight, arg0, arg1, 0.0F, 0.0F, arg2, arg3);
	}

	@OriginalMember(owner = "client!tf", name = "a", descriptor = "()V")
	public static void setupRgbAlphaMode1Rendering() {
		MaterialManager.setMaterial(0, 0);
		configureOrthographicView();
		setTextureCombineRgbMode(1);
		setTextureCombineAlphaMode(1);
		setLightingEnabled(false);
		setDepthTestEnabled(false);
		setFogEnabled(false);
		resetTextureMatrix();
	}

	@OriginalMember(owner = "client!tf", name = "c", descriptor = "()V")
	public static void setupRgbAlphaMode0Rendering() {
		MaterialManager.setMaterial(0, 0);
		configureOrthographicView();
		setTextureCombineRgbMode(0);
		setTextureCombineAlphaMode(0);
		setLightingEnabled(false);
		setDepthTestEnabled(false);
		setFogEnabled(false);
		resetTextureMatrix();
	}

	@OriginalMember(owner = "client!tf", name = "i", descriptor = "()V")
	public static void setupRenderingWithNoTexture() {
		MaterialManager.setMaterial(0, 0);
		configureOrthographicView();
		setTextureId(-1);
		setLightingEnabled(false);
		setDepthTestEnabled(false);
		setFogEnabled(false);
		resetTextureMatrix();
	}

	@OriginalMember(owner = "client!tf", name = "b", descriptor = "()V")
	public static void resetTextureMatrix() {
		if (textureMatrixModified) {
			glMatrixMode(GL2.GL_TEXTURE);
			glLoadIdentity();
			glMatrixMode(GL2.GL_MODELVIEW);
			textureMatrixModified = false;
		}
	}

	@OriginalMember(owner = "client!tf", name = "d", descriptor = "()V")
	public static void swapBuffers() {
		try {
			if ( !glfwWindowShouldClose(LWJGLwindow) ) {
				//glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer
				glfwSwapBuffers(LWJGLwindow); // swap the color buffers

				// Poll for window events. The key callback above will only be
				// invoked during this call.
				glfwPollEvents();
			}
		} catch (@Pc(3) Exception local3) {
		}
	}

	@OriginalMember(owner = "client!tf", name = "a", descriptor = "(Z)V")
	public static void setFogEnabled(@OriginalArg(0) boolean enabled) {
		if (enabled == fogEnabled) {
			return;
		}
		if (enabled) {
			glEnable(GL2.GL_FOG);
		} else {
			glDisable(GL2.GL_FOG);
		}
		fogEnabled = enabled;
	}

	@OriginalMember(owner = "client!tf", name = "f", descriptor = "()V")
	private static void resetOpenGLState() {
		isOrthoViewConfigured = false;
		glDisable(GL2.GL_TEXTURE_2D);
		textureId = -1;
		glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_COMBINE);
		glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_COMBINE_RGB, GL2.GL_MODULATE);
		textureCombineRgbMode = 0;
		glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_COMBINE_ALPHA, GL2.GL_MODULATE);
		textureCombineAlphaMode = 0;
		glEnable(GL2.GL_LIGHTING);
		glEnable(GL2.GL_FOG);
		glEnable(GL2.GL_DEPTH_TEST);
		lightingEnabled = true;
		depthTestEnabled = true;
		fogEnabled = true;
		resetMaterial();
		glActiveTexture(GL2.GL_TEXTURE1);
		glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_COMBINE);
		glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_COMBINE_RGB, GL2.GL_MODULATE);
		glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_COMBINE_ALPHA, GL2.GL_MODULATE);
		glActiveTexture(GL2.GL_TEXTURE0);
		//setSwapInterval(0);
		glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
		glShadeModel(GL2.GL_SMOOTH);
		glClearDepth(1.0D);
		glDepthFunc(GL2.GL_LEQUAL);
		enableDepthMask();
		glMatrixMode(GL2.GL_TEXTURE);
		glLoadIdentity();
		glPolygonMode(GL2.GL_FRONT, GL2.GL_FILL);
		glEnable(GL2.GL_CULL_FACE);
		glCullFace(GL2.GL_BACK);
		glEnable(GL2.GL_BLEND);										// Enable the OpenGL Blending functionality
		glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);	// Set the blend mode to blend our current RGBA with what is already in the buffer
		glEnable(GL2.GL_ALPHA_TEST);
		glAlphaFunc(GL2.GL_GREATER, 0.0F);
		glEnableClientState(GL2.GL_VERTEX_ARRAY);
		glEnableClientState(GL2.GL_NORMAL_ARRAY);
		normalArrayEnabled = true;
		glEnableClientState(GL2.GL_COLOR_ARRAY);
		glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
		glMatrixMode(GL2.GL_MODELVIEW);
		glLoadIdentity();
		FogManager.setup();
		LightingManager.resetLightingState();
	}

	@OriginalMember(owner = "client!tf", name = "g", descriptor = "()V")
	public static void enableDepthMask() { glDepthMask(true); }

	@OriginalMember(owner = "client!tf", name = "n", descriptor = "()V")
	public static void clearDepthBuffer() { glClear(GL2.GL_DEPTH_BUFFER_BIT); }

	@OriginalMember(owner = "client!tf", name = "q", descriptor = "()V")
	public static void disableDepthMask() { glDepthMask(false); }

	@OriginalMember(owner = "client!tf", name = "r", descriptor = "()F")
	public static float method4179() { return depthAdjustmentParameter; }

	@OriginalMember(owner = "client!tf", name = "l", descriptor = "()F")
	public static float method4166() { return depthAdjustmentFactor; }

	@OriginalMember(owner = "client!gj", name = "b", descriptor = "(I)V")
	public static void resetMaterial() {
		MaterialManager.setMaterial(0, 0);
	}

	@OriginalMember(owner = "client!tf", name = "b", descriptor = "(Z)V")
	public static void setDepthTestEnabled(@OriginalArg(0) boolean enabled) {
		if (enabled == depthTestEnabled) {
			return;
		}
		if (enabled) {
			glEnable(GL2.GL_DEPTH_TEST);
		} else {
			glDisable(GL2.GL_DEPTH_TEST);
		}
		depthTestEnabled = enabled;
	}

	@OriginalMember(owner = "client!tf", name = "h", descriptor = "()V")
	public static void draw() {
		@Pc(2) int[] ints = new int[2];
		ints[0] = GL11.glGetInteger(GL11.GL_DRAW_BUFFER);
		ints[1] = GL11.glGetInteger(GL11.GL_READ_BUFFER);
		glDrawBuffer(GL2.GL_BACK_LEFT);
		glReadBuffer(GL2.GL_FRONT_LEFT);
		setTextureId(-1);
		glPushAttrib(GL2.GL_ENABLE_BIT);
		glDisable(GL2.GL_FOG);
		glDisable(GL2.GL_BLEND);
		glDisable(GL2.GL_DEPTH_TEST);
		glDisable(GL2.GL_ALPHA_TEST);
		glRasterPos2i(0, 0);
		glCopyPixels(0, 0, canvasWidth, canvasHeight, GL2.GL_COLOR);
		glPopAttrib();
		glDrawBuffer(ints[0]);
		glReadBuffer(ints[1]);
	}

	@OriginalMember(owner = "client!tf", name = "j", descriptor = "()V")
	private static void configureOrthographicView() {
		if (isOrthoViewConfigured) {
			return;
		}
		glMatrixMode(GL2.GL_PROJECTION);		// Switch to the projection matrix so that we can manipulate how our scene is viewed
		glLoadIdentity();					// Reset the projection matrix to the identity matrix so that we don't get any artifacts (cleaning up)
		glOrtho(0, canvasWidth, 0, canvasHeight, -1.0D, 1.0D);
		setViewportBounds(0, 0, canvasWidth, canvasHeight);
		glMatrixMode(GL2.GL_MODELVIEW);		// Switch back to the model view matrix, so that we can start drawing shapes correctly
		glLoadIdentity();					// Reset the projection matrix to the identity matrix so that we don't get any artifacts (cleaning up)
		isOrthoViewConfigured = true;
	}

	@OriginalMember(owner = "client!tf", name = "c", descriptor = "(Z)V")
	public static void setLightingEnabled(@OriginalArg(0) boolean enabled) {
		if (enabled == lightingEnabled) {
			return;
		}
		if (enabled) {
			glEnable(GL2.GL_LIGHTING);
		} else {
			glDisable(GL2.GL_LIGHTING);
		}
		lightingEnabled = enabled;
	}

	@OriginalMember(owner = "client!tf", name = "o", descriptor = "()V")
	public static void quit() {
		if (gl != null) {
			try {
				MaterialManager.quit(); // MaterialManager
			} catch (@Pc(5) Throwable local5) {
			}
			// Release LWJGL
			// Free the window callbacks and destroy the window
			glfwFreeCallbacks(LWJGLwindow);
			glfwDestroyWindow(LWJGLwindow);

			// Terminate GLFW and free the error callback
			glfwTerminate();
			glfwSetErrorCallback(null).free();
		}

		if (window != null) {
			if (!window.getLock().isLocked()) {
				window.lockSurface();
			}

			if (context != null) {
				GlCleaner.clear(); // GlCleaner
				try {
					if (GLContext.getCurrent() == context) {
						context.release();
					}
				} catch (@Pc(17) Throwable ex) {
				}
				try {
					context.destroy();
				} catch (@Pc(21) Throwable ex) {
				}
			}
		}

		if (drawable != null) {
			try {
				drawable.setRealized(false);
			} catch (@Pc(30) Throwable ex) {
			}
		}
		window = null;
		gl = null;
		context = null;
		drawable = null;
		LightingManager.releaseLighting();
		enabled = false;
	}

	@OriginalMember(owner = "client!tf", name = "a", descriptor = "(FFF)V")
	public static void translateTextureMatrix(@OriginalArg(0) float x, @OriginalArg(1) float y, @OriginalArg(2) float z) {
		glMatrixMode(GL2.GL_TEXTURE);
		if (textureMatrixModified) {
			glLoadIdentity();
		}
		glTranslatef(x, y, z);
		glMatrixMode(GL2.GL_MODELVIEW);
		textureMatrixModified = true;
	}

	@OriginalMember(owner = "client!tf", name = "a", descriptor = "(IIIIIIFFII)V")
	public static void setupViewTransformations(@OriginalArg(0) int boxX, @OriginalArg(1) int boxY, @OriginalArg(2) int boxWidth, @OriginalArg(3) int boxHeight, @OriginalArg(4) int offsetX, @OriginalArg(5) int offsetY, @OriginalArg(6) float rotationX, @OriginalArg(7) float rotationY, @OriginalArg(8) int scaleX, @OriginalArg(9) int scaleY) {
		@Pc(7) int scaledBoxStartX = (boxX - offsetX << 8) / scaleX;
		@Pc(17) int scaledBoxEndX = (boxX + boxWidth - offsetX << 8) / scaleX;
		@Pc(25) int scaledBoxStartY = (boxY - offsetY << 8) / scaleY;
		@Pc(35) int scaledBoxEndY = (boxY + boxHeight - offsetY << 8) / scaleY;
		glMatrixMode(GL2.GL_PROJECTION);
		glLoadIdentity();
		configureProjectionMatrix((float) scaledBoxStartX * projectionCoordinateScaleFactor, (float) scaledBoxEndX * projectionCoordinateScaleFactor, (float) -scaledBoxEndY * projectionCoordinateScaleFactor, (float) -scaledBoxStartY * projectionCoordinateScaleFactor, 50.0F, (float) GlobalConfig.VIEW_DISTANCE);
		setViewportBounds(boxX, canvasHeight - boxY - boxHeight, boxWidth, boxHeight);
		glMatrixMode(GL2.GL_MODELVIEW);
		glLoadIdentity();
		glRotatef(180.0F, 1.0F, 0.0F, 0.0F);
		if (rotationX != 0.0F) {
			glRotatef(rotationX, 1.0F, 0.0F, 0.0F);
		}
		if (rotationY != 0.0F) {
			glRotatef(rotationY, 0.0F, 1.0F, 0.0F);
		}
		isOrthoViewConfigured = false;
		Rasteriser.screenLowerX = scaledBoxStartX;
		Rasteriser.screenUpperX = scaledBoxEndX;
		Rasteriser.screenLowerY = scaledBoxStartY;
		Rasteriser.screenUpperY = scaledBoxEndY;
	}

	@OriginalMember(owner = "client!tf", name = "d", descriptor = "(Z)V")
	private static void setNormalArrayEnabled(@OriginalArg(0) boolean enabled) {
		if (enabled == normalArrayEnabled) {
			return;
		}
		if (enabled) {
			glEnableClientState(GL2.GL_NORMAL_ARRAY);
		} else {
			glDisableClientState(GL2.GL_NORMAL_ARRAY);
		}
		normalArrayEnabled = enabled;
	}

	@OriginalMember(owner = "client!tf", name = "p", descriptor = "()V")
	public static void restoreLighting() {
		if (Preferences.highDetailLighting) {
			setLightingEnabled(true);
			setNormalArrayEnabled(true);
		} else {
			setLightingEnabled(false);
			setNormalArrayEnabled(false);
		}
	}

	@OriginalMember(owner = "client!tf", name = "a", descriptor = "(I)V")
	public static void setTextureCombineAlphaMode(@OriginalArg(0) int mode) {
		if (mode == textureCombineAlphaMode) {
			return;
		}
		if (mode == 0) {
			glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_COMBINE_ALPHA, GL2.GL_MODULATE);
		}
		if (mode == 1) {
			glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_COMBINE_ALPHA, GL2.GL_REPLACE);
		}
		if (mode == 2) {
			glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_COMBINE_ALPHA, GL2.GL_ADD);
		}
		textureCombineAlphaMode = mode;
	}

	@OriginalMember(owner = "client!tf", name = "a", descriptor = "(FFFFFF)V")
	private static void configureProjectionMatrix(@OriginalArg(0) float xMin, @OriginalArg(1) float xMax, @OriginalArg(2) float yMin, @OriginalArg(3) float yMax, @OriginalArg(4) float nearClip, @OriginalArg(5) float farClip) {
		float width = xMax - xMin;
		float height = yMax - yMin;

		hFOV = 2 * (float)Math.atan(width / (2 * nearClip));
		vFOV = 2 * (float)Math.atan(height / (2 * nearClip));
		hFOV = (float)Math.toDegrees(hFOV);
		vFOV = (float)Math.toDegrees(vFOV);

		@Pc(3) float doubleNearClip = nearClip * 2.0F;
		matrix[0] = doubleNearClip / (xMax - xMin);
		matrix[1] = 0.0F;
		matrix[2] = 0.0F;
		matrix[3] = 0.0F;
		matrix[4] = 0.0F;
		matrix[5] = doubleNearClip / (yMax - yMin);
		matrix[6] = 0.0F;
		matrix[7] = 0.0F;
		matrix[8] = (xMax + xMin) / (xMax - xMin);
		matrix[9] = (yMax + yMin) / (yMax - yMin);
		matrix[10] = depthScaleFactor = -(farClip + nearClip) / (farClip - nearClip);
		matrix[11] = -1.0F;
		matrix[12] = 0.0F;
		matrix[13] = 0.0F;
		matrix[14] = scaledFarClipDistance = -(doubleNearClip * farClip) / (farClip - nearClip);
		matrix[15] = 0.0F;
		glLoadMatrixf(matrix);
		depthAdjustmentParameter = 0.0F;
		depthAdjustmentFactor = 0.0F;
	}

	@OriginalMember(owner = "client!tf", name = "a", descriptor = "(F)V")
	public static void configureFixedDepthAdjustment(@OriginalArg(0) float multiplier) {
		configureDepthAdjustment(3000.0F, multiplier * 1.5F);
	}

	@OriginalMember(owner = "client!tf", name = "a", descriptor = "(FF)V")
	public static void configureDepthAdjustment(@OriginalArg(0) float arg0, @OriginalArg(1) float arg1) {
		if (isOrthoViewConfigured || arg0 == depthAdjustmentParameter && arg1 == depthAdjustmentFactor) {
			return;
		}
		depthAdjustmentParameter = arg0;
		depthAdjustmentFactor = arg1;
		if (arg1 == 0.0F) {
			matrix[10] = depthScaleFactor;
			matrix[14] = scaledFarClipDistance;
		} else {
			@Pc(25) float depthRatio = arg0 / (arg1 + arg0);
			@Pc(29) float depthRatioSquared = depthRatio * depthRatio;
			@Pc(42) float depthAdjustment = -scaledFarClipDistance * (1.0F - depthRatio) * (1.0F - depthRatio) / arg1;
			matrix[10] = depthScaleFactor + depthAdjustment;
			matrix[14] = scaledFarClipDistance * depthRatioSquared;
		}
		glMatrixMode(GL2.GL_PROJECTION);
		glLoadMatrixf(matrix);
		glMatrixMode(GL2.GL_MODELVIEW);
	}

	@OriginalMember(owner = "client!tf", name = "b", descriptor = "(I)V")
	public static void clearColorAndDepthBuffers(@OriginalArg(0) int rgb) {
		glClearColor((float) (rgb >> 16 & 0xFF) / 255.0F, (float) (rgb >> 8 & 0xFF) / 255.0F, (float) (rgb & 0xFF) / 255.0F, 0.0F);
		glClear(GL2.GL_DEPTH_BUFFER_BIT | GL2.GL_COLOR_BUFFER_BIT);
		glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
	}

	@OriginalMember(owner = "client!tf", name = "c", descriptor = "(I)V")
	public static void setTextureId(@OriginalArg(0) int id) {
		if (id == textureId) {
			return;
		}
		if (id == -1) {
			glDisable(GL2.GL_TEXTURE_2D);
		} else {
			if (textureId == -1) {
				glEnable(GL2.GL_TEXTURE_2D);
			}
			glBindTexture(GL2.GL_TEXTURE_2D, id);
		}
		textureId = id;
	}

	private static void initLWJGL() {
		// Setup an error callback. The default implementation
		// will print the error message in System.err.
		GLFWErrorCallback.createPrint(System.err).set();

		// Initialize GLFW. Most GLFW functions will not work before doing this.
		if ( !glfwInit() )
			throw new IllegalStateException("Unable to initialize GLFW");

		// Configure GLFW
		glfwDefaultWindowHints(); // optional, the current window hints are already the default
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

		// Create the window
		LWJGLwindow = glfwCreateWindow(300, 300, "Hello World!", NULL, NULL);
		if ( LWJGLwindow == NULL )
			throw new RuntimeException("Failed to create the GLFW window");

		// Setup a key callback. It will be called every time a key is pressed, repeated or released.
		glfwSetKeyCallback(LWJGLwindow, (window, key, scancode, action, mods) -> {
			if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
				glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
		});

		// Get the thread stack and push a new frame
		try ( MemoryStack stack = stackPush() ) {
			IntBuffer pWidth = stack.mallocInt(1); // int*
			IntBuffer pHeight = stack.mallocInt(1); // int*

			// Get the window size passed to glfwCreateWindow
			glfwGetWindowSize(LWJGLwindow, pWidth, pHeight);

			// Get the resolution of the primary monitor
			GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

			// Center the window
			glfwSetWindowPos(
					LWJGLwindow,
					(vidmode.width() - pWidth.get(0)) / 2,
					(vidmode.height() - pHeight.get(0)) / 2
			);
		} // the stack frame is popped automatically

		// Make the OpenGL context current
		glfwMakeContextCurrent(LWJGLwindow);
		// Enable v-sync
		glfwSwapInterval(1);

		// Make the window visible
		glfwShowWindow(LWJGLwindow);

		GL.createCapabilities();
	}

	@OriginalMember(owner = "client!tf", name = "a", descriptor = "(Ljava/awt/Canvas;I)I")
	public static int init(@OriginalArg(0) Canvas canvas, @OriginalArg(1) int numSamples) {
		try {
			if (!canvas.isDisplayable()) {
				return -1;
			}

			// Create JOGL
			GLProfile profile = GLProfile.get(GLProfile.GL3bc);
			@Pc(8) GLCapabilities capabilities = new GLCapabilities(profile);
			if (numSamples > 0) {
				capabilities.setSampleBuffers(true);
				capabilities.setNumSamples(numSamples * 4);
			}
			@Pc(18) GLDrawableFactory factory = GLDrawableFactory.getFactory(profile);
			AWTGraphicsConfiguration config = AWTGraphicsConfiguration.create(canvas.getGraphicsConfiguration(), capabilities, capabilities);
			window = NewtFactoryAWT.getNativeWindow(canvas, config);
			if (!window.getLock().isLocked()) {
				window.lockSurface();
			}
			try {
				drawable = factory.createGLDrawable(window);
				drawable.setRealized(true);
			} finally {
				window.unlockSurface();
			}
			@Pc(29) int swapBuffersAttempts = 0;
			@Pc(36) int result;

			while (true) {
				context = drawable.createContext(null);
				try {
					result = context.makeCurrent();
					if (result != 0) {
						break;
					}
				} catch (@Pc(41) Exception local41) {
				}
				if (swapBuffersAttempts++ > 5) {
					return -2;
				}
			}

			// Create LWJGL (I think we snatch the context in the thread from the above code)...
			System.out.println("Hello LWJGL " + Version.getVersion() + "!");
			initLWJGL();

			gl = GLContext.getCurrentGL().getGL2();
			glLineWidth((float) GameShell.canvasScale);

			enabled = true;
			canvasWidth = canvas.getSize().width;
			canvasHeight = canvas.getSize().height;


			if (LWJGLwindow == 0) {
				quit();
			}
			genTextures();
			resetOpenGLState();
			glClear(GL2.GL_COLOR_BUFFER_BIT);
			swapBuffersAttempts = 0;
			while (true) {
				try {
					// Main draw loop
					drawable.swapBuffers();
					break;
				} catch (@Pc(86) Exception ex) {
					if (swapBuffersAttempts++ > 5) {
						quit();
						return -3;
					}
					ThreadUtils.sleep(100L);
				}
			}
			glClear(GL2.GL_COLOR_BUFFER_BIT);
			return 0;
		} catch (@Pc(103) Throwable ex) {
			quit();
			return -5;
		}
	}

	@OriginalMember(owner = "client!tf", name = "a", descriptor = "(II)V")
	public static void setCanvasSize(@OriginalArg(0) int width, @OriginalArg(1) int height) {
		canvasWidth = width;
		canvasHeight = height;
		isOrthoViewConfigured = false;
	}

	public static void setViewportBounds(@OriginalArg(0) int x, @OriginalArg(1) int y, @OriginalArg(2) int width, @OriginalArg(3) int height) {
		leftMargin = x;
		topMargin = y;
		viewportWidth = width;
		viewportHeight = height;
		resizeViewport();
	}

	@OriginalMember(owner = "client!gi", name = "b", descriptor = "()V")
	private static void resizeViewport() {
		glViewport((int) (leftMargin * GameShell.canvasScale + GameShell.subpixelX), (int) (topMargin * GameShell.canvasScale + GameShell.subpixelY),
			(int) (viewportWidth * GameShell.canvasScale + GameShell.subpixelX), (int) (viewportHeight * GameShell.canvasScale + GameShell.subpixelY));
	}

	@OriginalMember(owner = "client!tf", name = "a", descriptor = "(IIIIII)V")
	public static void setupOrthographicProjection(@OriginalArg(0) int xOffset, @OriginalArg(1) int yOffset, @OriginalArg(2) int resolution, @OriginalArg(3) int arg3, @OriginalArg(4) int color, @OriginalArg(5) int cardMemory) {
		@Pc(2) int negXOffset = -xOffset;
		@Pc(6) int adjustedCanvasWidth = canvasWidth - xOffset;
		@Pc(9) int negYOffset = -yOffset;
		@Pc(13) int adjustedCanvasHeight = canvasHeight - yOffset;
		@Pc(23) float resolutionFactor = (float) resolution / 512.0F;
		@Pc(30) float colorDepthFactor = resolutionFactor * (256.0F / (float) color);
		@Pc(37) float memoryFactor = resolutionFactor * (256.0F / (float) cardMemory);
		glMatrixMode(GL2.GL_PROJECTION);
		glLoadIdentity();
		glOrtho((float) negXOffset * colorDepthFactor, (float) adjustedCanvasWidth * colorDepthFactor, (float) -adjustedCanvasHeight * memoryFactor, (float) -negYOffset * memoryFactor, 50 - arg3, GlobalConfig.VIEW_DISTANCE - arg3);
		setViewportBounds(0, 0, canvasWidth, canvasHeight);
		glMatrixMode(GL2.GL_MODELVIEW);
		glLoadIdentity();
		glRotatef(180.0F, 1.0F, 0.0F, 0.0F);
		isOrthoViewConfigured = false;
	}

	@OriginalMember(owner = "client!tf", name = "d", descriptor = "(I)V")
	public static void setTextureCombineRgbMode(@OriginalArg(0) int mode) {
		if (mode == textureCombineRgbMode) {
			return;
		}
		if (mode == 0) {
			glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_COMBINE_RGB, GL2.GL_MODULATE);
		}
		if (mode == 1) {
			glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_COMBINE_RGB, GL2.GL_REPLACE);
		}
		if (mode == 2) {
			glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_COMBINE_RGB, GL2.GL_ADD);
		}
		if (mode == 3) {
			glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_COMBINE_RGB, GL2.GL_SUBTRACT);
		}
		if (mode == 4) {
			glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_COMBINE_RGB, GL2.GL_ADD_SIGNED);
		}
		if (mode == 5) {
			glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_COMBINE_RGB, GL2.GL_INTERPOLATE);
		}
		textureCombineRgbMode = mode;
	}

	@OriginalMember(owner = "client!tf", name = "s", descriptor = "()V")
	private static void genTextures() {
		@Pc(2) int[] local2 = new int[1];
		glGenTextures(local2);
		anInt5328 = local2[0];
		glBindTexture(GL2.GL_TEXTURE_2D, anInt5328);
		glTexImage2D(GL2.GL_TEXTURE_2D, 0, 4, 1, 1, 0, GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, IntBuffer.wrap(new int[]{-1}));
		LightingManager.init();
		MaterialManager.init();
	}

}
