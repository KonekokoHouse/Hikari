#version 410 core

#moj_import < minecraft: dynamictransforms.glsl >
#moj_import <minecraft: projection.glsl >

layout (location = 0) in vec3 a_Position;
layout (location = 1) in vec4 a_Color;
layout (location = 2) in vec4 a_InnerRect; // ROUND_INNER_RECT (Now treated as Outer Bounds)
layout (location = 3) in vec4 a_Radius; // ROUND_RADIUS

out vec2 f_Position;
out vec4 f_Color;
flat out vec4 f_InnerRect;
flat out vec4 f_Radius;

void main() {
gl_Position = ProjMat * ModelViewMat * vec4(a_Position, 1.0);

f_Position = a_Position.xy;
f_Color = a_Color;

f_InnerRect = a_InnerRect;
f_Radius = a_Radius;
}
