#version 330 core

precision lowp float;

in vec2 uv;
out vec4 color;

uniform sampler2D uTexture;
uniform vec2 uHalfTexelSize;
uniform float uOffset;
uniform vec2 uUvScale;
uniform vec2 uUvOffset;

void main() {
    vec2 baseUv = uv * uUvScale + uUvOffset;
    color = (
        texture(uTexture, baseUv) * 4.0 +
        texture(uTexture, baseUv - uHalfTexelSize.xy * uOffset) +
        texture(uTexture, baseUv + uHalfTexelSize.xy * uOffset) +
        texture(uTexture, baseUv + vec2(uHalfTexelSize.x, -uHalfTexelSize.y) * uOffset) +
        texture(uTexture, baseUv - vec2(uHalfTexelSize.x, -uHalfTexelSize.y) * uOffset)
    ) / 8.0;
    color.a = 1.0;
}
