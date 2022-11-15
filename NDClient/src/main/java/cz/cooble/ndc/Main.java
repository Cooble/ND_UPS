package cz.cooble.ndc;

import cz.cooble.ndc.graphics.*;
import org.joml.Matrix4f;
import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Main {

    // The window handle
    private long window;

    public void run() {
        System.out.println("Hello LWJGL " + Version.getVersion() + "!");

        init();
        loop();

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    private Texture texture;
    private VertexBuffer vbo;
    private VertexArray vao;
    private Shader shader;


    float vertices[] = {
            -0.5f, -0.5f, 0.0f,     0,0,
            0.5f, -0.5f, 0.0f,      1,1,
            0.0f, 0.5f, 0.0f,        1,0,
    };

    private void init() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW
        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

        // Create the window
        window = glfwCreateWindow(800, 800, "Hello World!", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
        });

        // Get the thread stack and push a new frame
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        } // the stack frame is popped automatically

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(window);
    }

    Matrix4f modelMatrix = new Matrix4f().identity();

    FrameBuffer fbo;


    float rotate= 0;
    private void loop() {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();

        glfwSwapBuffers(window);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        var b = BufferUtils.createFloatBuffer(3* (3+2));
        b.put(vertices);
        b.flip();

        texture = new Texture(new Texture.Info().path("C:\\Users\\minek\\Pictures\\Video Projects\\Capture.png"));
        vbo = new VertexBuffer(b, VertexBuffer.Usage.STATIC_DRAW);
        vbo.setLayout(new VertexBuffer.Layout(
                new VertexBuffer.Element(g_typ.VEC3),
                new VertexBuffer.Element(g_typ.VEC2)));
        vao = new VertexArray();
        vao.addBuffer(vbo);
        Shader.Source source = new Shader.Source();

        source.vertex = """
                #version 330 core
                layout (location = 0) in vec3 position;
                layout (location = 1) in vec2 uv;

                out vec2 v_uv;
                
                uniform mat4 u_worldMatrix;
                uniform mat4 u_projectionMatrix;
                uniform mat4 u_viewMatrix;
                                
                void main()
                {
                    gl_Position = /*u_projectionMatrix * u_viewMatrix * u_worldMatrix **/ vec4(position,1.f);
                    v_uv = uv;
                }
                """;

        source.fragment = """
                #version 330 core
                
                uniform sampler2D u_texture;
                
                layout(location=0) out vec4 color;
                
                in vec2 v_uv;
                
                void main(){
                	vec4 cc = texture2D(u_texture, v_uv);
                	color = vec4(1.f,1.f,1.f,1.f);
                }
                """;


        shader = new Shader(source);
        shader.bind();
        shader.setUniformMat4("u_worldMatrix",new Matrix4f().identity());
        shader.setUniformMat4("u_projectionMatrix",new Matrix4f().identity());
        shader.setUniformMat4("u_viewMatrix",new Matrix4f().identity());
        shader.setUniform1i("u_texture",0);
        shader.unbind();


        // Set the clear color
        GCon.clearColor(0.0f, 1.0f, 0.0f, 1.0f);

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while (!glfwWindowShouldClose(window)) {
            GCon.clear(GCon.BufferBit.COLOR | GCon.BufferBit.DEPTH); // clear the framebuffer
           // modelMatrix.rotate(0.01f,0,0,1);

            //shader.setUniformMat4("u_worldMatrix",modelMatrix);

            texture.bind(0);
            shader.bind();
            vao.bind();
            GCon.cmdDrawArrays(GCon.Topology.TRIANGLES, 0, 3);


           /* glUseProgram(shader.id());
            glBindVertexArray(vao.id());
            glDrawArrays(GL_TRIANGLES, 0, 3);
            glBindVertexArray(0);*/


            glfwSwapBuffers(window); // swap the color buffers

            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents();
        }
        end();
    }

    private void end() {
        vao.delete();
        shader.delete();
        vbo.delete();
        if (texture != null)
            texture.delete();
    }

    public static void main(String[] args) {
        new Main().run();
    }

}