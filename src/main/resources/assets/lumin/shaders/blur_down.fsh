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
        texture(Sampler0, baseUv) * 4.0 +
        texture(Sampler0, baseUv - uHalfTexelSize.xy * uOffset) +
        texture(Sampler0, baseUv + uHalfTexelSize.xy * uOffset) +
        texture(Sampler0, baseUv + vec2(uHalfTexelSize.x, -uHalfTexelSize.y) * uOffset) +
        texture(Sampler0, baseUv - vec2(uHalfTexelSize.x, -uHalfTexelSize.y) * uOffset)
    ) / 8.0;
    fragColor.a = 1.0;
}
