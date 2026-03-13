#version 410 core

#moj_import < minecraft: dynamictransforms.glsl >
#moj_import <minecraft: projection.glsl >

layout (location = 0) in vec3 Position;
layout (location = 1) in vec4 Color;

out vec4 v_Color;

void main() {
gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
v_Color = Color;
}
