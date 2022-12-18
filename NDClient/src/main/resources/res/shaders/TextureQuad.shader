#shader vertex

#version 400 core
layout(location = 0) in vec2 position;
layout(location = 1) in vec2 uv;
uniform mat4 u_transform;
uniform mat4 u_transform_uv;

out vec2 f_uv;

void main() {
	gl_Position = u_transform * vec4(position,0,1);
	f_uv = (u_transform_uv * vec4(uv,0,1)).xy;
}


#shader fragment
#version 400 core

layout(location = 0) out vec4 color;

in vec2 f_uv;

uniform sampler2D u_attachment;

void main() {
	color = texture2D(u_attachment, f_uv);
}

