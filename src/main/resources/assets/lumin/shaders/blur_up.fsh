#version 410 core

precision mediump float;

uniform sampler2D Sampler0;

layout(std140) uniform BlurInfo {
    vec2 uHalfTexelSize;
    float uOffset;
    float _padding;
    vec2 uUvScale;
    vec2 uUvOffset;
};

in vec2 v_TexCoord;
out vec4 fragColor;

void main() {
    vec2 baseUv = v_TexCoord * uUvScale + uUvOffset;
    fragColor = (
        texture(Sampler0, baseUv + vec2(-uHalfTexelSize.x * 2.0, 0.0) * uOffset) +
        texture(Sampler0, baseUv + vec2(-uHalfTexelSize.x, uHalfTexelSize.y) * uOffset) * 2.0 +
        texture(Sampler0, baseUv + vec2(0.0, uHalfTexelSize.y * 2.0) * uOffset) +
        texture(Sampler0, baseUv + uHalfTexelSize * uOffset) * 2.0 +
        texture(Sampler0, baseUv + vec2(uHalfTexelSize.x * 2.0, 0.0) * uOffset) +
        texture(Sampler0, baseUv + vec2(uHalfTexelSize.x, -uHalfTexelSize.y) * uOffset) * 2.0 +
        texture(Sampler0, baseUv + vec2(0.0, -uHalfTexelSize.y * 2.0) * uOffset) +
        texture(Sampler0, baseUv - uHalfTexelSize * uOffset) * 2.0
    ) / 12.0;
    fragColor.a = 1.0;
}
