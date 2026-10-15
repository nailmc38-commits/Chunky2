package com.secretcpvp.amethystglow;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public final class GlowRenderer {
    private static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("amethystglow", "pipeline/amethyst_through_walls"))
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .build());

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1, 1, 1, 1);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private final ByteBufferBuilder allocator = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);
    private MappableRingBuffer gpuVertices;

    public void register() {
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(this::render);
    }

    private void render(WorldRenderContext context) {
        GlowConfig settings = AmethystGlow.CONFIG;
        if (!settings.enabled || context.matrices() == null) return;
        List<BlockPos> blocks = AmethystGlow.SCAN.positions();
        if (blocks.isEmpty()) return;

        Vec3 cam = context.worldState().cameraRenderState.pos;
        PoseStack stack = context.matrices();
        stack.pushPose();
        stack.translate(-cam.x, -cam.y, -cam.z);
        Matrix4fc matrix = stack.last().pose();

        BufferBuilder builder = new BufferBuilder(allocator, PIPELINE.getVertexFormatMode(), PIPELINE.getVertexFormat());
        float alpha = settings.opacity / 100f;
        for (BlockPos pos : blocks) {
            if (pos.distToCenterSqr(cam.x, cam.y, cam.z) > (settings.radius + 2) * (settings.radius + 2)) continue;
            renderCube(matrix, builder,
                    pos.getX() + 0.025f, pos.getY() + 0.025f, pos.getZ() + 0.025f,
                    pos.getX() + 0.975f, pos.getY() + 0.975f, pos.getZ() + 0.975f,
                    0.72f, 0.28f, 1.0f, alpha);
        }
        stack.popPose();

        MeshData mesh = builder.buildOrThrow();
        try {
            MeshData.DrawState info = mesh.drawState();
            VertexFormat format = info.format();
            int bytes = info.vertexCount() * format.getVertexSize();
            if (bytes == 0) return;
            if (gpuVertices == null || gpuVertices.size() < bytes) {
                if (gpuVertices != null) gpuVertices.close();
                gpuVertices = new MappableRingBuffer(() -> "Amethyst Glow vertices",
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, bytes);
            }

            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            try (GpuBuffer.MappedView mapped = encoder.mapBuffer(
                    gpuVertices.currentBuffer().slice(0, mesh.vertexBuffer().remaining()), false, true)) {
                MemoryUtil.memCopy(mesh.vertexBuffer(), mapped.data());
            }
            mesh.sortQuads(allocator, RenderSystem.getProjectionType().vertexSorting());
            GpuBuffer indexBuffer = format.uploadImmediateIndexBuffer(mesh.indexBuffer());
            GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(
                    RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);

            try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                    () -> "Amethyst Glow overlay",
                    Minecraft.getInstance().getMainRenderTarget().getColorTextureView(), OptionalInt.empty(),
                    Minecraft.getInstance().getMainRenderTarget().getDepthTextureView(), OptionalDouble.empty())) {
                pass.setPipeline(PIPELINE);
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", transforms);
                pass.setVertexBuffer(0, gpuVertices.currentBuffer());
                pass.setIndexBuffer(indexBuffer, mesh.drawState().indexType());
                pass.drawIndexed(0, 0, info.indexCount(), 1);
            }
            gpuVertices.rotate();
        } finally {
            mesh.close();
        }
    }

    private static void renderCube(Matrix4fc matrix, BufferBuilder out,
            float x0, float y0, float z0, float x1, float y1, float z1,
            float r, float g, float b, float a) {
        // +Z
        vertex(matrix, out, x0,y0,z1,r,g,b,a);
        vertex(matrix, out, x1,y0,z1,r,g,b,a);
        vertex(matrix, out, x1,y1,z1,r,g,b,a);
        vertex(matrix, out, x0,y1,z1,r,g,b,a);
        // -Z
        vertex(matrix, out, x1,y0,z0,r,g,b,a);
        vertex(matrix, out, x0,y0,z0,r,g,b,a);
        vertex(matrix, out, x0,y1,z0,r,g,b,a);
        vertex(matrix, out, x1,y1,z0,r,g,b,a);
        // -X
        vertex(matrix, out, x0,y0,z0,r,g,b,a);
        vertex(matrix, out, x0,y0,z1,r,g,b,a);
        vertex(matrix, out, x0,y1,z1,r,g,b,a);
        vertex(matrix, out, x0,y1,z0,r,g,b,a);
        // +X
        vertex(matrix, out, x1,y0,z1,r,g,b,a);
        vertex(matrix, out, x1,y0,z0,r,g,b,a);
        vertex(matrix, out, x1,y1,z0,r,g,b,a);
        vertex(matrix, out, x1,y1,z1,r,g,b,a);
        // +Y
        vertex(matrix, out, x0,y1,z1,r,g,b,a);
        vertex(matrix, out, x1,y1,z1,r,g,b,a);
        vertex(matrix, out, x1,y1,z0,r,g,b,a);
        vertex(matrix, out, x0,y1,z0,r,g,b,a);
        // -Y
        vertex(matrix, out, x0,y0,z0,r,g,b,a);
        vertex(matrix, out, x1,y0,z0,r,g,b,a);
        vertex(matrix, out, x1,y0,z1,r,g,b,a);
        vertex(matrix, out, x0,y0,z1,r,g,b,a);
    }

    private static void vertex(Matrix4fc matrix, BufferBuilder builder,
                               float x, float y, float z, float r, float g, float b, float a) {
        builder.addVertex(matrix, x, y, z).setColor(r, g, b, a);
    }

    public void close() {
        if (gpuVertices != null) {
            gpuVertices.close();
            gpuVertices = null;
        }
        allocator.close();
    }
}
