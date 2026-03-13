#version 410 core

#moj_import < minecraft: dynamictransforms.glsl >
#moj_import <minecraft: projection.glsl >

layout (location = 0) in vec3 a_Position;
layout (location = 1) in vec4 a_Color;
layout (location = 2) in vec4 a_InnerRect;
layout (location = 3) in vec4 a_Radius;

out vec2 f_Position;
out vec4 f_Color;
flat out vec4 f_Bounds;
flat out vec4 f_Radius;
flat out float f_BlurRadius;

void main() {
gl_Position = ProjMat * ModelViewMat * vec4(a_Position.xy, 0.0, 1.0);

f_Position = a_Position.xy;
f_Color = a_Color;
f_Bounds = a_InnerRect;
f_Radius = a_Radius;
f_BlurRadius = a_Position.z;
}
