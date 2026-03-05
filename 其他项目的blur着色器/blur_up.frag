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
        texture(uTexture, baseUv + vec2(-uHalfTexelSize.x * 2.0, 0.0) * uOffset) +
        texture(uTexture, baseUv + vec2(-uHalfTexelSize.x, uHalfTexelSize.y) * uOffset) * 2.0 +
        texture(uTexture, baseUv + vec2(0.0, uHalfTexelSize.y * 2.0) * uOffset) +
        texture(uTexture, baseUv + uHalfTexelSize * uOffset) * 2.0 +
        texture(uTexture, baseUv + vec2(uHalfTexelSize.x * 2.0, 0.0) * uOffset) +
        texture(uTexture, baseUv + vec2(uHalfTexelSize.x, -uHalfTexelSize.y) * uOffset) * 2.0 +
        texture(uTexture, baseUv + vec2(0.0, -uHalfTexelSize.y * 2.0) * uOffset) +
        texture(uTexture, baseUv - uHalfTexelSize * uOffset) * 2.0
    ) / 12.0;
    color.a = 1.0;
}
