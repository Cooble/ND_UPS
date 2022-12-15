package cz.cooble.ndc.graphics;

import cz.cooble.ndc.input.*;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFWCharCallbackI;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static cz.cooble.ndc.Asserter.ND_TRACE;
import static cz.cooble.ndc.Asserter.ND_WARN;
import static cz.cooble.ndc.graphics.Window.WindowCursor.CURSOR_DISABLED;
import static cz.cooble.ndc.graphics.Window.WindowCursor.CURSOR_HIDDEN;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {
    private long m_window;
    private static boolean is_glfw_initialized;
    private boolean m_destroyed;
    private boolean m_raw_mouse_enabled;
    private WindowCursor m_cursor_policy;
    private boolean m_iconified;

    public FrameBuffer getFBO() {
        return m_window_fbo;
    }

    public boolean isIconified() {
        return m_iconified;
    }

    public boolean isFocused() {
        return m_data.focused;
    }

    public enum WindowCursor {
        CURSOR_DISABLED(0),
        CURSOR_ENABLED(1),
        CURSOR_HIDDEN(2);

        public final int i;

        WindowCursor(int i) {
            this.i = i;
        }
    }

    private static class WindowData {
        int width, height;
        int lastWidth, lastHeight;
        int x, y;
        int lastX, lastY;
        boolean fullscreen = false;
        String title;
        EventCallback eventCallback;
        boolean focused;
        boolean hovered;
    }

    FrameBuffer m_window_fbo;
    WindowData m_data = new WindowData();

    private Vector2f getMousePosition() {
        try (MemoryStack stack = stackPush()) {
            var pX = stack.mallocDouble(1);
            var pY = stack.mallocDouble(1);
            glfwGetCursorPos(m_window, pX, pY);
            return new Vector2f((float) pX.get(), (float) (getHeight()-pY.get()));
        }
    }

    public Window(int width, int height, String title, boolean fullscreen) {

        m_data.width = width;
        m_data.height = height;
        m_data.title = title;
        m_data.eventCallback = event -> {
        };

        if (!is_glfw_initialized) {
            is_glfw_initialized = true;
            if (!glfwInit())
                throw new IllegalStateException("Unable to initialize GLFW");


        }
        glfwDefaultWindowHints(); // optional, the current window hints are already the default

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

        glfwWindowHint(GLFW_SAMPLES, 4);

        m_window = glfwCreateWindow(width, height, title, 0, 0);
        if (m_window == NULL) {
            glfwTerminate();
            throw new RuntimeException("Failed to create the GLFW window");
        }

       /* // load icons
        // --------------------
        {
            setIcon(ND_RESLOC("res/engine/images/nd_icon2.png"));
        }*/
        // Get the thread stack and push a new frame

        glfwMakeContextCurrent(m_window);
        glfwSwapInterval(0); //vsync off

        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();

        //glfwSetWindowUserPointer()
        //glfwSetWindowUserPointer(m_window, &m_data);

        glfwSetFramebufferSizeCallback(m_window, (window, width1, height1) -> {
            glViewport(0, 0, width1, height1);
            //m_window_fbo.resize(width1,height1);
            m_data.width = width1;
            m_data.height = height1;
            var e = new WindowResizeEvent(width, height);
            m_data.eventCallback.consume(e);
        });

        glfwSetWindowCloseCallback(m_window, window -> {
            m_data.eventCallback.consume(new WindowCloseEvent());
        });
        glfwSetWindowFocusCallback(m_window, (l, focused) -> m_data.focused = focused);

        //==========key===========
        glfwSetKeyCallback(m_window, (l, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS || action == GLFW_REPEAT) {
                var e = new KeyPressEvent(key, mods, false, action == GLFW_REPEAT);
                m_data.eventCallback.consume(e);
            } else if (action == GLFW_RELEASE) {
                var e = new KeyPressEvent(key, mods, true, false);
                m_data.eventCallback.consume(e);
            }
        });
        glfwSetCharCallback(m_window, (l, key) -> {
            var e = new KeyTypeEvent(key, 0);
            m_data.eventCallback.consume(e);
        });

        //==========mouse===========

        glfwSetMouseButtonCallback(m_window, (l, button, action, mods) -> {
            if (action == GLFW_PRESS || action == GLFW_RELEASE) {
                var e = new MousePressEvent(getMousePosition(), mods, MouseCode.values()[button], action == GLFW_RELEASE);
                m_data.eventCallback.consume(e);
            }
        });

        glfwSetCursorPosCallback(m_window, (l, x, y) -> {
            var e = new MouseMoveEvent(getMousePosition());
            m_data.eventCallback.consume(e);
        });
        glfwSetScrollCallback(m_window, (l, x, y) -> {
            var e = new MouseScrollEvent(getMousePosition(), 0, (float) x, (float) y);
            m_data.eventCallback.consume(e);
        });
       /* glfwSetCursorEnterCallback(m_window, (l, b) -> {
            WindowData& d = *(WindowData*)glfwGetWindowUserPointer(window);
            d.hovered = entered;
            double x, y;
            glfwGetCursorPos(window, &x, &y);
            MouseEnteredEvent e(x, y, (entered));
            d.eventCallback(e);
        });*/


        if (fullscreen)
            setFullScreen(true);

        ND_TRACE("Window created with dimensions: [{}, {}], fullscreen: {}", width, height, fullscreen);
        m_raw_mouse_enabled = glfwRawMouseMotionSupported();
        if (!m_raw_mouse_enabled) {
            ND_WARN("Window does not support raw mouse input!");
        }
        //set the default fbo to be this window target
        m_window_fbo = new FrameBuffer(new FrameBuffer.Info().windowTarget());
        Renderer.setDefaultFBO(m_window_fbo); //default fbo is in case of opengl "empty" => directly rendered to screen

        // Make the window visible
        glfwShowWindow(m_window);
    }

    public void setEventCallback(EventCallback callback) {
        m_data.eventCallback = callback;
    }


    Vector2i getPos() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer pX = stack.mallocInt(1);
            IntBuffer pY = stack.mallocInt(1);

            glfwGetWindowPos(m_window, pX, pY);

            return new Vector2i(pX.get(), pY.get());
        }
    }

   public Vector2i getDimensions() {
            return new Vector2i(m_data.width, m_data.height);
    }
    public int getWidth(){
        return m_data.width;
    }
    public int getHeight(){
        return m_data.height;
    }

    public void setSize(int width, int height) {
        m_data.width = width;
        m_data.height = height;
        glfwSetWindowSize(m_window, width, height);
    }

    public void setFullScreen(boolean fullscreen) {
        if (m_data.fullscreen == fullscreen)
            return;
        if (fullscreen) {
            try (MemoryStack stack = stackPush()) {
                IntBuffer pX = stack.mallocInt(1);
                IntBuffer pY = stack.mallocInt(1);
                IntBuffer pW = stack.mallocInt(1);
                IntBuffer pH = stack.mallocInt(1);
                glfwGetWindowPos(m_window, pX, pY);
                glfwGetWindowSize(m_window, pW, pH);

                m_data.lastX = pX.get();
                m_data.lastY = pY.get();
                m_data.lastWidth = pW.get();
                m_data.lastHeight = pH.get();

                glfwSetWindowMonitor(m_window, glfwGetPrimaryMonitor(), 0, 0, 1920, 1080, 60);
            }
        } else {
            glfwSetWindowMonitor(m_window, NULL, m_data.lastX, m_data.lastY, m_data.lastWidth, m_data.lastHeight, 60);
            m_data.width = m_data.lastWidth;
            m_data.height = m_data.lastHeight;
            m_data.x = m_data.lastX;
            m_data.y = m_data.lastY;
        }
        m_data.fullscreen = fullscreen;
    }

    public void setTitle(String title) {
        m_data.title = title;
        glfwSetWindowTitle(m_window, title);
    }

    public void setCursorPolicy(WindowCursor state) {
        var s = GLFW_CURSOR_NORMAL;
        if (state == CURSOR_DISABLED)
            s = GLFW_CURSOR_DISABLED;
        else if (state == CURSOR_HIDDEN)
            s = GLFW_CURSOR_HIDDEN;
        glfwSetInputMode(m_window, GLFW_CURSOR, s);
        if (m_raw_mouse_enabled)
            glfwSetInputMode(m_window, GLFW_RAW_MOUSE_MOTION, s == GLFW_CURSOR_DISABLED ? 1 : 0);
        m_cursor_policy = state;
    }

    void setCursorPos(Vector2f pos) {
        glfwSetCursorPos(m_window, pos.x, pos.y);
    }

    void setClipboard(String c) {
        glfwSetClipboardString(NULL, c);
    }

    public void close() {
        if (!m_destroyed) {
            glfwFreeCallbacks(m_window);
            glfwDestroyWindow(m_window);
            //glfwSetErrorCallback(null);
            glfwTerminate();
        }
        m_destroyed = true;
    }

    public void swapBuffers() {
        glfwSwapBuffers(m_window);
        glClearColor(1, 0, 1, 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    public void pollEvents() {
        glfwPollEvents();
        m_iconified = glfwGetWindowAttrib(m_window, GLFW_ICONIFIED) != 0;
    }

    public boolean shouldClose() {
        return m_window != 0 && glfwWindowShouldClose(m_window);
    }

    /*public void setIcon(String image_path) {
        stbi_set_flip_vertically_on_load(false);

        int w, h, mbbpp;
        GLFWimage icons[ 1];
        if (FUtil::exists (image_path))
        {
            icons -> pixels = stbi_load(std::string (image_path).c_str(), &w, &h, &mbbpp, 4);
            icons -> width = w;
            icons -> height = h;

            glfwSetWindowIcon(m_window, 1, icons);
            stbi_image_free(icons[0].pixels);
        }
    }*/

    public String getClipboard() {
        return glfwGetClipboardString(NULL);
    }

    public static class RealInput {

        private List<Integer> m_keys=new ArrayList<>();
        private List<Integer> m_mouse_keys=new ArrayList<>();
        private Window m_window;
        private Vector2f m_drag_offset = new Vector2f(0, 0);

        public RealInput(Window window) {
            this.m_window = window;

        }

        //==============================INPUT==============================================
        public int getKey(int button) {
            if (button >= m_keys.size()) {
                var lastSize = m_keys.size();
                for (int i = 0; i < (button + 2) - lastSize; ++i)  //clear all new keys
                    m_keys.add(0);

                m_keys.set(button, 0);
            }
            return m_keys.get(button);
        }

        public int getMouseKey(MouseCode button) {
            var btn = button.ordinal();
            if (btn >= m_mouse_keys.size()) {
                var lastSize = m_mouse_keys.size();
                for (int i = 0; i < (btn + 2) - lastSize; ++i)  //clear all new keys
                    m_mouse_keys.add(0);

                m_mouse_keys.set(btn, 0);
            }
            return m_mouse_keys.get(btn);
        }

        public void update() {
            for (int i = 0; i < m_keys.size(); ++i) {
                var k = getKey(i);
                if (isKeyPressed(i)) {
                    if (k < 127)
                        ++k;
                } else if (k > 0)
                    k = -1;
                else k = 0;
            }
            for (int i = 0; i < m_mouse_keys.size(); ++i) {
                var k = getMouseKey(MouseCode.values()[i]);
                if (isMousePressed(MouseCode.values()[i])) {
                    if (k < 127)
                        ++k;
                } else if (k > 0)
                    k = -1;
                else k = 0;
            }
            if (isMouseFreshlyPressed(MouseCode.LEFT) || isMouseFreshlyPressed(MouseCode.MIDDLE) || isMouseFreshlyPressed(MouseCode.RIGHT))
                m_drag_offset = getMouseLocation();
        }

        public boolean isKeyPressed(int button) {
            getKey(button); //save this button to vector
            var state = glfwGetKey(m_window.m_window, button);
            return state == GLFW_PRESS || state == GLFW_REPEAT;
        }

        public boolean isKeyFreshlyPressed(int button) {
            return getKey(button) == 1;
        }

        public boolean isKeyFreshlyReleased(int button) {
            return getKey(button) == -1;
        }

        public boolean isMousePressed(MouseCode button) {
            getMouseKey(button); //save this button to vector
            var state = glfwGetMouseButton(m_window.m_window, button.ordinal());
            return state == GLFW_PRESS;
        }

        public boolean isMouseFreshlyPressed(MouseCode button) {
            return getMouseKey(button) == 1;
        }

        public boolean isMouseFreshlyReleased(MouseCode button) {
            return getMouseKey(button) == -1;
        }

        public Vector2f getDragging() {
            return getMouseLocation().min(m_drag_offset);
        }

        public Vector2f getMouseLocation() {
            try (MemoryStack stack = stackPush()) {
                var pX = stack.mallocDouble(1);
                var pY = stack.mallocDouble(1);
                glfwGetCursorPos(m_window.m_window, pX, pY);
                return new Vector2f((float) pX.get(), (float) pY.get());
            }
        }
    }
}
