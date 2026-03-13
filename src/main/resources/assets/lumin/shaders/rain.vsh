#version 410 core

#moj_import < minecraft: dynamictransforms.glsl >
#moj_import <minecraft: projection.glsl >

layout (location = 0) in vec3 Position;
layout (location = 1) in vec2 Uv0;
layout (location = 2) in vec4 Color;

out vec2 f_TexCoord;
out vec4 f_Color;

void main() {
gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
f_TexCoord = Uv0;
f_Color = Color;
}
