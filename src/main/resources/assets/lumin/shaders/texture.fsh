#version 410 core

smooth in vec2 f_Position;
smooth in vec4 f_Color;
smooth in vec2 f_TexCoord;
flat in vec4 f_InnerRect;
flat in vec4 f_Radius;

layout(location = 0) out vec4 fragColor;

uniform sampler2D Sampler0;

float aastep(float x) {
    float afwidth = fwidth(x);
    return smoothstep(-afwidth, afwidth, x);
}

void main() {
    vec4 tex = texture(Sampler0, f_TexCoord);
    vec4 color = tex * f_Color;

    vec2 center = (f_InnerRect.xy + f_InnerRect.zw) / 2.0;
    vec2 halfSize = (f_InnerRect.zw - f_InnerRect.xy) / 2.0;
    vec2 pos = f_Position - center;

    // Determine which corner we are in to select the correct radius
    // Top-Left: xy < 0, Top-Right: x > 0, y < 0
    // Bottom-Left: x < 0, y > 0, Bottom-Right: x > 0, y > 0
    // Note: f_Position Y increases downwards usually in Minecraft GUI?
    // Let's assume standard coordinates.
    // Actually simpler:
    // Radius: x=TopLeft, y=TopRight, z=BottomRight, w=BottomLeft
    
    // Check against edges
    // If we are "inside" the inner rect, distance is < 0.
    // If we are "outside", we need to check rounded corners.
    
    // We can use a Signed Distance Field approach for a rounded box with varying radii.
    // udRoundRect( p, b, r )
    // float udRoundRect( vec2 p, vec2 b, float r ) {
    //   return length(max(abs(p)-b+r,0.0))-r;
    // }
    // But r varies per quadrant.
    
    // Select radius based on quadrant relative to center
    // Assuming f_Radius order: x=TL, y=TR, z=BR, w=BL
    
    // In screen space (y down):
    // TL: x < center.x, y < center.y
    // TR: x > center.x, y < center.y
    // BR: x > center.x, y > center.y
    // BL: x < center.x, y > center.y
    
    float r = 0.0;
    if (pos.x < 0.0) {
        if (pos.y < 0.0) r = f_Radius.x; // TL
        else r = f_Radius.w; // BL
    } else {
        if (pos.y < 0.0) r = f_Radius.y; // TR
        else r = f_Radius.z; // BR
    }
    
    // Distance to rounded box
    // q = abs(p) - b + r
    // dist = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r
    
    // However, our "InnerRect" definition in Java code was:
    // innerX1 = x + radius; innerY1 = y + radius; ...
    // This was defining the "inner safe zone".
    // But now with varying radii, the "inner safe zone" is complex.
    // It's better to pass the OUTER bounds and the RADII.
    // But to keep compatibility with existing VertexFormat layout (InnerRect + Radius),
    // let's reinterpret the meaning or adjust the shader.
    
    // Current Java logic:
    // InnerRect = (x+r, y+r, x2-r, y2-r)
    // This assumes uniform radius.
    
    // If we want varying radii, we should probably pass the OUTER Rect (x, y, x2, y2) instead of InnerRect?
    // OR we just calculate dist from edges.
    
    // Let's assume f_InnerRect is actually passing (x, y, x2, y2) - The Outer Bounds.
    // We will need to update Java code to pass outer bounds instead of inner.
    
    // Let's check Java code:
    // writeVertex(... innerX1, innerY1, innerX2, innerY2 ...)
    // innerX1 = x + radius
    
    // If we change Java to pass x, y, x2, y2 as "InnerRect" (renamed to Bounds maybe?),
    // then:
    // vec2 tl = f_Position - f_InnerRect.xy; // Vector from TL
    // vec2 br = f_InnerRect.zw - f_Position; // Vector to BR
    // vec2 d = min(tl, br); // Distance to closest edge (positive inside)
    
    // If d.x < r && d.y < r: we are in a corner area.
    
    // Let's adapt the shader to use standard SDF for rounded rect with varying radii.
    // We need the half-size (b) and the center.
    // And we need the radii.
    
    // If we change f_InnerRect to be (x, y, x2, y2) [Outer Bounds]
    // center = (Bounds.xy + Bounds.zw) * 0.5
    // size = (Bounds.zw - Bounds.xy) * 0.5
    
    // Let's assume we will update Java to pass Outer Bounds in locations 3.
    
    vec2 halfSize2 = (f_InnerRect.zw - f_InnerRect.xy) * 0.5;
    vec2 center2 = (f_InnerRect.xy + f_InnerRect.zw) * 0.5;
    vec2 p = f_Position - center2;
    
    // Select radius r for the current quadrant of p
    // Radius vector: x=TL, y=TR, z=BR, w=BL
    // p.x < 0, p.y < 0 -> TL -> r = f_Radius.x
    // p.x > 0, p.y < 0 -> TR -> r = f_Radius.y
    // p.x > 0, p.y > 0 -> BR -> r = f_Radius.z
    // p.x < 0, p.y > 0 -> BL -> r = f_Radius.w
    
    // Note: This selection logic works if p is relative to center.
    
    float r_current = (p.x > 0.0) ? 
        ((p.y > 0.0) ? f_Radius.z : f_Radius.y) : 
        ((p.y > 0.0) ? f_Radius.w : f_Radius.x);
        
    // SDF for rounded box
    // d = length(max(abs(p) - size + r, 0.0)) - r
    
    vec2 q = abs(p) - halfSize2 + r_current;
    float dist = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r_current;
    
    // dist < 0 is inside, > 0 is outside
    // We want alpha = 1 inside, 0 outside.
    // smoothstep approach
    
    float alpha = 1.0 - smoothstep(-0.5, 0.5, dist); // Simple AA
    
    if (alpha < 0.001 || color.a < 0.01) discard;
    fragColor = vec4(color.rgb, color.a * alpha);
}
