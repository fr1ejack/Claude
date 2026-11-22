// AUTOMATISCH ERZEUGT von tools/modelgen.py – nicht von Hand bearbeiten.
package de.hiveinvasion.client.model;

import net.minecraft.client.model.*;

public final class GenModels {
    private GenModels() {}

    public static TexturedModelData stalker() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        ModelPartData p_left_leg = root.addChild("left_leg", ModelPartBuilder.create().uv(32, 0).cuboid(-1.5f, 0.0f, -1.5f, 3.0f, 7.0f, 3.0f), ModelTransform.of(2.6f, 10.0f, 1.0f, -0.4363323129985824f, 0.0f, 0.0f));
        ModelPartData p_left_shin = p_left_leg.addChild("left_shin", ModelPartBuilder.create().uv(94, 0).cuboid(-1.0f, 0.0f, -1.0f, 2.0f, 7.0f, 2.0f), ModelTransform.of(0.0f, 6.5f, 0.0f, 0.9599310885968813f, 0.0f, 0.0f));
        ModelPartData p_left_foot = p_left_shin.addChild("left_foot", ModelPartBuilder.create().uv(86, 22).cuboid(-1.5f, 0.0f, -3.5f, 3.0f, 1.0f, 5.0f), ModelTransform.of(0.0f, 6.5f, 0.0f, -0.5235987755982988f, 0.0f, 0.0f));
        ModelPartData p_right_leg = root.addChild("right_leg", ModelPartBuilder.create().uv(44, 0).cuboid(-1.5f, 0.0f, -1.5f, 3.0f, 7.0f, 3.0f), ModelTransform.of(-2.6f, 10.0f, 1.0f, -0.4363323129985824f, 0.0f, 0.0f));
        ModelPartData p_right_shin = p_right_leg.addChild("right_shin", ModelPartBuilder.create().uv(102, 0).cuboid(-1.0f, 0.0f, -1.0f, 2.0f, 7.0f, 2.0f), ModelTransform.of(0.0f, 6.5f, 0.0f, 0.9599310885968813f, 0.0f, 0.0f));
        ModelPartData p_right_foot = p_right_shin.addChild("right_foot", ModelPartBuilder.create().uv(102, 22).cuboid(-1.5f, 0.0f, -3.5f, 3.0f, 1.0f, 5.0f), ModelTransform.of(0.0f, 6.5f, 0.0f, -0.5235987755982988f, 0.0f, 0.0f));
        ModelPartData p_body = root.addChild("body", ModelPartBuilder.create().uv(0, 13).cuboid(-3.0f, -5.0f, -2.0f, 6.0f, 5.0f, 4.0f).uv(6, 0).cuboid(-4.0f, -12.0f, -2.5f, 8.0f, 7.0f, 5.0f).uv(0, 0).cuboid(-1.0f, -12.0f, 2.5f, 2.0f, 12.0f, 1.0f), ModelTransform.of(0.0f, 11.0f, 1.0f, 0.6108652381980153f, 0.0f, 0.0f));
        ModelPartData p_head = p_body.addChild("head", ModelPartBuilder.create().uv(56, 0).cuboid(-2.5f, -3.0f, -6.0f, 5.0f, 4.0f, 6.0f).uv(20, 13).cuboid(-2.5f, -4.0f, -1.5f, 5.0f, 4.0f, 5.0f).uv(68, 13).cuboid(-2.0f, -4.5f, 3.0f, 4.0f, 4.0f, 4.0f).uv(16, 22).cuboid(-1.5f, -4.5f, 6.5f, 3.0f, 3.0f, 4.0f).uv(78, 30).cuboid(-1.0f, -4.2f, 10.0f, 2.0f, 2.0f, 3.0f).uv(16, 37).cuboid(-2.0f, 0.5f, -6.2f, 4.0f, 1.0f, 1.0f).uv(88, 30).cuboid(-1.8f, -2.5f, -6.4f, 3.6f, 3.0f, 1.0f), ModelTransform.of(0.0f, -12.0f, -1.0f, -0.5235987755982988f, 0.0f, 0.0f));
        ModelPartData p_jaw = p_head.addChild("jaw", ModelPartBuilder.create().uv(0, 30).cuboid(-2.0f, 0.0f, -5.5f, 4.0f, 1.0f, 5.0f).uv(26, 37).cuboid(-1.8f, -1.0f, -5.6f, 3.6f, 1.0f, 1.0f), ModelTransform.of(0.0f, 1.0f, -0.5f, 0.13962634015954636f, 0.0f, 0.0f));
        ModelPartData p_left_arm = p_body.addChild("left_arm", ModelPartBuilder.create().uv(78, 0).cuboid(-1.0f, -1.0f, -1.0f, 2.0f, 8.0f, 2.0f).uv(18, 30).cuboid(-0.5f, -1.5f, -1.5f, 2.0f, 3.0f, 3.0f), ModelTransform.of(5.2f, -9.5f, 0.0f, -0.3490658503988659f, 0.0f, -0.13962634015954636f));
        ModelPartData p_left_forearm = p_left_arm.addChild("left_forearm", ModelPartBuilder.create().uv(52, 13).cuboid(-0.75f, 0.0f, -0.75f, 1.5f, 7.0f, 1.5f), ModelTransform.of(0.0f, 7.0f, 0.0f, -0.6108652381980153f, 0.0f, 0.0f));
        ModelPartData p_left_hand = p_left_forearm.addChild("left_hand", ModelPartBuilder.create().uv(0, 37).cuboid(-1.0f, 0.0f, -1.0f, 2.0f, 1.0f, 2.0f).uv(106, 30).cuboid(-1.0f, 1.0f, -1.5f, 0.5f, 3.0f, 0.5f).uv(110, 30).cuboid(0.5f, 1.0f, -1.5f, 0.5f, 3.0f, 0.5f).uv(98, 30).cuboid(-0.25f, 1.0f, -1.5f, 0.5f, 3.5f, 0.5f), ModelTransform.of(0.0f, 7.0f, 0.0f, 0.17453292519943295f, 0.0f, 0.0f));
        ModelPartData p_right_arm = p_body.addChild("right_arm", ModelPartBuilder.create().uv(86, 0).cuboid(-1.0f, -1.0f, -1.0f, 2.0f, 8.0f, 2.0f).uv(28, 30).cuboid(-1.5f, -1.5f, -1.5f, 2.0f, 3.0f, 3.0f), ModelTransform.of(-5.2f, -9.5f, 0.0f, -0.3490658503988659f, 0.0f, 0.13962634015954636f));
        ModelPartData p_right_forearm = p_right_arm.addChild("right_forearm", ModelPartBuilder.create().uv(60, 13).cuboid(-0.75f, 0.0f, -0.75f, 1.5f, 7.0f, 1.5f), ModelTransform.of(0.0f, 7.0f, 0.0f, -0.6108652381980153f, 0.0f, 0.0f));
        ModelPartData p_right_hand = p_right_forearm.addChild("right_hand", ModelPartBuilder.create().uv(8, 37).cuboid(-1.0f, 0.0f, -1.0f, 2.0f, 1.0f, 2.0f).uv(114, 30).cuboid(-1.0f, 1.0f, -1.5f, 0.5f, 3.0f, 0.5f).uv(118, 30).cuboid(0.5f, 1.0f, -1.5f, 0.5f, 3.0f, 0.5f).uv(102, 30).cuboid(-0.25f, 1.0f, -1.5f, 0.5f, 3.5f, 0.5f), ModelTransform.of(0.0f, 7.0f, 0.0f, 0.17453292519943295f, 0.0f, 0.0f));
        ModelPartData p_tail0 = p_body.addChild("tail0", ModelPartBuilder.create().uv(0, 22).cuboid(-1.1f, -1.1f, 0.0f, 2.2f, 2.2f, 5.0f), ModelTransform.of(0.0f, 0.0f, 2.0f, -0.8726646259971648f, 0.0f, 0.0f));
        ModelPartData p_tail1 = p_tail0.addChild("tail1", ModelPartBuilder.create().uv(30, 22).cuboid(-0.9750000000000001f, -0.9750000000000001f, 0.0f, 1.9500000000000002f, 1.9500000000000002f, 5.0f), ModelTransform.of(0.0f, 0.0f, 4.5f, 0.13962634015954636f, 0.0f, 0.0f));
        ModelPartData p_tail2 = p_tail1.addChild("tail2", ModelPartBuilder.create().uv(44, 22).cuboid(-0.8500000000000001f, -0.8500000000000001f, 0.0f, 1.7000000000000002f, 1.7000000000000002f, 5.0f), ModelTransform.of(0.0f, 0.0f, 4.5f, 0.13962634015954636f, 0.0f, 0.0f));
        ModelPartData p_tail3 = p_tail2.addChild("tail3", ModelPartBuilder.create().uv(58, 22).cuboid(-0.7250000000000001f, -0.7250000000000001f, 0.0f, 1.4500000000000002f, 1.4500000000000002f, 5.0f), ModelTransform.of(0.0f, 0.0f, 4.5f, 0.13962634015954636f, 0.0f, 0.0f));
        ModelPartData p_tail4 = p_tail3.addChild("tail4", ModelPartBuilder.create().uv(72, 22).cuboid(-0.6000000000000001f, -0.6000000000000001f, 0.0f, 1.2000000000000002f, 1.2000000000000002f, 5.0f), ModelTransform.of(0.0f, 0.0f, 4.5f, 0.13962634015954636f, 0.0f, 0.0f));
        ModelPartData p_tail5 = p_tail4.addChild("tail5", ModelPartBuilder.create().uv(38, 30).cuboid(-0.4750000000000001f, -0.4750000000000001f, 0.0f, 0.9500000000000002f, 0.9500000000000002f, 5.0f), ModelTransform.of(0.0f, 0.0f, 4.5f, 0.13962634015954636f, 0.0f, 0.0f));
        ModelPartData p_tail_blade = p_tail5.addChild("tail_blade", ModelPartBuilder.create().uv(40, 13).cuboid(-0.25f, -2.0f, 0.0f, 0.5f, 4.0f, 5.0f), ModelTransform.of(0.0f, 0.0f, 4.5f, 0.0f, 0.0f, 0.0f));
        ModelPartData p_tubes = p_body.addChild("tubes", ModelPartBuilder.create().uv(84, 13).cuboid(-3.5f, -1.0f, 0.0f, 1.5f, 1.5f, 6.0f).uv(100, 13).cuboid(2.0f, -1.0f, 0.0f, 1.5f, 1.5f, 6.0f).uv(50, 30).cuboid(-2.8f, 2.0f, 0.0f, 1.2f, 1.2f, 4.5f).uv(64, 30).cuboid(1.6f, 2.0f, 0.0f, 1.2f, 1.2f, 4.5f), ModelTransform.of(0.0f, -11.0f, 2.5f, 0.5235987755982988f, 0.0f, 0.0f));
        return TexturedModelData.of(data, 128, 64);
    }

    public static TexturedModelData clinger() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        ModelPartData p_body = root.addChild("body", ModelPartBuilder.create().uv(0, 0).cuboid(-3.0f, -1.5f, -4.0f, 6.0f, 2.5f, 7.5f).uv(28, 0).cuboid(-2.0f, -2.3f, -3.0f, 4.0f, 1.0f, 5.0f).uv(22, 11).cuboid(-1.5f, 0.8f, -3.0f, 3.0f, 0.7f, 3.0f), ModelTransform.of(0.0f, 21.8f, 0.0f, 0.0f, 0.0f, 0.0f));
        ModelPartData p_leg0 = p_body.addChild("leg0", ModelPartBuilder.create().uv(34, 11).cuboid(0.0f, -0.5f, -0.5f, 5.0f, 1.0f, 1.0f), ModelTransform.of(3.0f, 0.3f, -3.2f, 0.0f, 0.8726646259971648f, 0.3490658503988659f));
        ModelPartData p_leg0_tip = p_leg0.addChild("leg0_tip", ModelPartBuilder.create().uv(12, 19).cuboid(0.0f, -0.4f, -0.4f, 4.0f, 0.8f, 0.8f), ModelTransform.of(5.0f, 0.0f, 0.0f, 0.0f, 0.0f, -0.9599310885968813f));
        ModelPartData p_leg1 = p_body.addChild("leg1", ModelPartBuilder.create().uv(46, 11).cuboid(0.0f, -0.5f, -0.5f, 5.0f, 1.0f, 1.0f), ModelTransform.of(3.0f, 0.3f, -1.3000000000000003f, 0.0f, 0.34906585039886595f, 0.3490658503988659f));
        ModelPartData p_leg1_tip = p_leg1.addChild("leg1_tip", ModelPartBuilder.create().uv(22, 19).cuboid(0.0f, -0.4f, -0.4f, 4.0f, 0.8f, 0.8f), ModelTransform.of(5.0f, 0.0f, 0.0f, 0.0f, 0.0f, -0.9599310885968813f));
        ModelPartData p_leg2 = p_body.addChild("leg2", ModelPartBuilder.create().uv(0, 17).cuboid(0.0f, -0.5f, -0.5f, 5.0f, 1.0f, 1.0f), ModelTransform.of(3.0f, 0.3f, 0.5999999999999996f, 0.0f, -0.17453292519943286f, 0.3490658503988659f));
        ModelPartData p_leg2_tip = p_leg2.addChild("leg2_tip", ModelPartBuilder.create().uv(32, 19).cuboid(0.0f, -0.4f, -0.4f, 4.0f, 0.8f, 0.8f), ModelTransform.of(5.0f, 0.0f, 0.0f, 0.0f, 0.0f, -0.9599310885968813f));
        ModelPartData p_leg3 = p_body.addChild("leg3", ModelPartBuilder.create().uv(12, 17).cuboid(0.0f, -0.5f, -0.5f, 5.0f, 1.0f, 1.0f), ModelTransform.of(3.0f, 0.3f, 2.499999999999999f, 0.0f, -0.6981317007977318f, 0.3490658503988659f));
        ModelPartData p_leg3_tip = p_leg3.addChild("leg3_tip", ModelPartBuilder.create().uv(42, 19).cuboid(0.0f, -0.4f, -0.4f, 4.0f, 0.8f, 0.8f), ModelTransform.of(5.0f, 0.0f, 0.0f, 0.0f, 0.0f, -0.9599310885968813f));
        ModelPartData p_leg4 = p_body.addChild("leg4", ModelPartBuilder.create().uv(24, 17).cuboid(-5.0f, -0.5f, -0.5f, 5.0f, 1.0f, 1.0f), ModelTransform.of(-3.0f, 0.3f, -3.2f, 0.0f, -0.8726646259971648f, -0.3490658503988659f));
        ModelPartData p_leg4_tip = p_leg4.addChild("leg4_tip", ModelPartBuilder.create().uv(52, 19).cuboid(-4.0f, -0.4f, -0.4f, 4.0f, 0.8f, 0.8f), ModelTransform.of(-5.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.9599310885968813f));
        ModelPartData p_leg5 = p_body.addChild("leg5", ModelPartBuilder.create().uv(36, 17).cuboid(-5.0f, -0.5f, -0.5f, 5.0f, 1.0f, 1.0f), ModelTransform.of(-3.0f, 0.3f, -1.3000000000000003f, 0.0f, -0.34906585039886595f, -0.3490658503988659f));
        ModelPartData p_leg5_tip = p_leg5.addChild("leg5_tip", ModelPartBuilder.create().uv(0, 21).cuboid(-4.0f, -0.4f, -0.4f, 4.0f, 0.8f, 0.8f), ModelTransform.of(-5.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.9599310885968813f));
        ModelPartData p_leg6 = p_body.addChild("leg6", ModelPartBuilder.create().uv(48, 17).cuboid(-5.0f, -0.5f, -0.5f, 5.0f, 1.0f, 1.0f), ModelTransform.of(-3.0f, 0.3f, 0.5999999999999996f, 0.0f, 0.17453292519943286f, -0.3490658503988659f));
        ModelPartData p_leg6_tip = p_leg6.addChild("leg6_tip", ModelPartBuilder.create().uv(10, 21).cuboid(-4.0f, -0.4f, -0.4f, 4.0f, 0.8f, 0.8f), ModelTransform.of(-5.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.9599310885968813f));
        ModelPartData p_leg7 = p_body.addChild("leg7", ModelPartBuilder.create().uv(0, 19).cuboid(-5.0f, -0.5f, -0.5f, 5.0f, 1.0f, 1.0f), ModelTransform.of(-3.0f, 0.3f, 2.499999999999999f, 0.0f, 0.6981317007977318f, -0.3490658503988659f));
        ModelPartData p_leg7_tip = p_leg7.addChild("leg7_tip", ModelPartBuilder.create().uv(20, 21).cuboid(-4.0f, -0.4f, -0.4f, 4.0f, 0.8f, 0.8f), ModelTransform.of(-5.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.9599310885968813f));
        ModelPartData p_tail = p_body.addChild("tail", ModelPartBuilder.create().uv(46, 0).cuboid(-0.75f, -0.75f, 0.0f, 1.5f, 1.5f, 4.0f), ModelTransform.of(0.0f, 0.0f, 3.5f, -0.08726646259971647f, 0.0f, 0.0f));
        ModelPartData p_tail2 = p_tail.addChild("tail2", ModelPartBuilder.create().uv(0, 11).cuboid(-0.6f, -0.6f, 0.0f, 1.2f, 1.2f, 4.0f), ModelTransform.of(0.0f, 0.0f, 4.0f, 0.0f, 0.0f, 0.0f));
        ModelPartData p_tail3 = p_tail2.addChild("tail3", ModelPartBuilder.create().uv(12, 11).cuboid(-0.45f, -0.45f, 0.0f, 0.9f, 0.9f, 4.0f), ModelTransform.of(0.0f, 0.0f, 4.0f, 0.0f, 0.0f, 0.0f));
        return TexturedModelData.of(data, 64, 32);
    }

    public static TexturedModelData larva() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        ModelPartData p_body = root.addChild("body", ModelPartBuilder.create(), ModelTransform.of(0.0f, 24.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        ModelPartData p_head = p_body.addChild("head", ModelPartBuilder.create().uv(18, 0).cuboid(-1.6f, -3.4f, -4.0f, 3.2f, 3.2f, 4.0f).uv(0, 8).cuboid(-1.2f, -1.4f, -4.4f, 2.4f, 0.8f, 0.6f), ModelTransform.of(0.0f, -0.2f, -2.5f, 0.0f, 0.0f, 0.0f));
        ModelPartData p_mid = p_body.addChild("mid", ModelPartBuilder.create().uv(0, 0).cuboid(-1.6f, -3.0f, 0.0f, 3.2f, 3.0f, 5.0f), ModelTransform.of(0.0f, 0.0f, -2.5f, 0.0f, 0.0f, 0.0f));
        ModelPartData p_back = p_mid.addChild("back", ModelPartBuilder.create().uv(34, 0).cuboid(-1.2f, -2.3f, 0.0f, 2.4f, 2.3f, 4.0f), ModelTransform.of(0.0f, 0.0f, 4.5f, 0.0f, 0.0f, 0.0f));
        ModelPartData p_tip = p_back.addChild("tip", ModelPartBuilder.create().uv(48, 0).cuboid(-0.8f, -1.5f, 0.0f, 1.6f, 1.5f, 3.0f), ModelTransform.of(0.0f, 0.0f, 4.0f, 0.0f, 0.0f, 0.0f));
        ModelPartData p_arm_l = p_mid.addChild("arm_l", ModelPartBuilder.create().uv(8, 8).cuboid(0.0f, -0.3f, -0.3f, 1.5f, 0.6f, 0.6f), ModelTransform.of(1.6f, -1.0f, 1.0f, 0.0f, 0.5235987755982988f, 0.5235987755982988f));
        ModelPartData p_arm_r = p_mid.addChild("arm_r", ModelPartBuilder.create().uv(14, 8).cuboid(-1.5f, -0.3f, -0.3f, 1.5f, 0.6f, 0.6f), ModelTransform.of(-1.6f, -1.0f, 1.0f, 0.0f, -0.5235987755982988f, -0.5235987755982988f));
        return TexturedModelData.of(data, 64, 32);
    }

    public static TexturedModelData exosuit() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        ModelPartData p_left_leg = root.addChild("left_leg", ModelPartBuilder.create().uv(32, 13).cuboid(-2.2f, 0.0f, -2.2f, 4.4f, 6.0f, 4.4f).uv(26, 43).cuboid(-1.6f, 5.0f, -1.6f, 3.2f, 2.0f, 3.2f).uv(10, 49).cuboid(-2.4f, 1.0f, -2.6f, 4.8f, 1.0f, 0.6f), ModelTransform.of(2.3f, 12.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        ModelPartData p_left_shin = p_left_leg.addChild("left_shin", ModelPartBuilder.create().uv(32, 24).cuboid(-2.0f, 0.0f, -2.0f, 4.0f, 5.0f, 4.0f).uv(58, 43).cuboid(-2.3f, 0.5f, -2.6f, 4.6f, 3.0f, 1.0f).uv(36, 34).cuboid(-2.4f, 4.5f, -3.6f, 4.8f, 1.5f, 5.8f), ModelTransform.of(0.0f, 6.5f, 0.0f, 0.0f, 0.0f, 0.0f));
        ModelPartData p_right_leg = root.addChild("right_leg", ModelPartBuilder.create().uv(52, 13).cuboid(-2.2f, 0.0f, -2.2f, 4.4f, 6.0f, 4.4f).uv(42, 43).cuboid(-1.6f, 5.0f, -1.6f, 3.2f, 2.0f, 3.2f).uv(22, 49).cuboid(-2.4f, 1.0f, -2.6f, 4.8f, 1.0f, 0.6f), ModelTransform.of(-2.3f, 12.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        ModelPartData p_right_shin = p_right_leg.addChild("right_shin", ModelPartBuilder.create().uv(48, 24).cuboid(-2.0f, 0.0f, -2.0f, 4.0f, 5.0f, 4.0f).uv(70, 43).cuboid(-2.3f, 0.5f, -2.6f, 4.6f, 3.0f, 1.0f).uv(58, 34).cuboid(-2.4f, 4.5f, -3.6f, 4.8f, 1.5f, 5.8f), ModelTransform.of(0.0f, 6.5f, 0.0f, 0.0f, 0.0f, 0.0f));
        ModelPartData p_body = root.addChild("body", ModelPartBuilder.create().uv(64, 24).cuboid(-4.5f, -3.0f, -2.6f, 9.0f, 3.0f, 5.2f).uv(88, 0).cuboid(-4.5f, -12.0f, 0.6f, 9.0f, 9.0f, 2.4f).uv(72, 13).cuboid(-1.5f, -12.0f, -0.6f, 3.0f, 9.0f, 1.4f), ModelTransform.of(0.0f, 12.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        ModelPartData p_jetpack = p_body.addChild("jetpack", ModelPartBuilder.create().uv(112, 0).cuboid(-4.0f, 0.0f, 0.0f, 3.0f, 8.0f, 3.0f).uv(0, 13).cuboid(1.0f, 0.0f, 0.0f, 3.0f, 8.0f, 3.0f).uv(96, 34).cuboid(-1.0f, 1.0f, 0.0f, 2.0f, 5.0f, 2.0f).uv(90, 43).cuboid(-3.5f, 8.0f, 0.5f, 2.0f, 1.0f, 2.0f).uv(98, 43).cuboid(1.5f, 8.0f, 0.5f, 2.0f, 1.0f, 2.0f), ModelTransform.of(0.0f, -11.0f, 3.0f, 0.0f, 0.0f, 0.0f));
        ModelPartData p_chest_left = p_body.addChild("chest_left", ModelPartBuilder.create().uv(0, 0).cuboid(-4.5f, -4.5f, -3.6f, 4.5f, 9.0f, 3.6f).uv(120, 43).cuboid(-4.0f, -2.5f, -3.9f, 3.0f, 2.0f, 0.4f).uv(40, 49).cuboid(-4.6f, 2.5f, -3.8f, 4.6f, 1.0f, 0.4f), ModelTransform.of(4.5f, -7.5f, 0.6f, 0.0f, 0.0f, 0.0f));
        ModelPartData p_chest_right = p_body.addChild("chest_right", ModelPartBuilder.create().uv(18, 0).cuboid(0.0f, -4.5f, -3.6f, 4.5f, 9.0f, 3.6f).uv(52, 49).cuboid(0.2f, -3.5f, -3.9f, 3.5f, 1.0f, 0.4f).uv(62, 49).cuboid(0.0f, 2.5f, -3.8f, 4.6f, 1.0f, 0.4f), ModelTransform.of(-4.5f, -7.5f, 0.6f, 0.0f, 0.0f, 0.0f));
        ModelPartData p_helmet = p_body.addChild("helmet", ModelPartBuilder.create().uv(36, 0).cuboid(-3.8f, -7.5f, -0.8f, 7.6f, 7.5f, 4.6f).uv(80, 34).cuboid(-1.0f, -8.6f, -2.0f, 2.0f, 1.2f, 6.0f), ModelTransform.of(0.0f, -12.2f, 0.5f, 0.0f, 0.0f, 0.0f));
        ModelPartData p_visor = p_helmet.addChild("visor", ModelPartBuilder.create().uv(62, 0).cuboid(-3.8f, 0.0f, -4.2f, 7.6f, 7.5f, 4.2f).uv(106, 43).cuboid(-3.0f, 2.2f, -4.5f, 6.0f, 2.2f, 0.5f).uv(0, 49).cuboid(-2.0f, 5.2f, -4.4f, 4.0f, 1.4f, 0.4f), ModelTransform.of(0.0f, -7.5f, -0.8f, 0.0f, 0.0f, 0.0f));
        ModelPartData p_left_arm = p_body.addChild("left_arm", ModelPartBuilder.create().uv(82, 13).cuboid(-2.0f, -2.0f, -2.0f, 4.0f, 6.0f, 4.0f).uv(94, 24).cuboid(-2.2f, -3.0f, -2.6f, 5.0f, 3.0f, 5.2f), ModelTransform.of(6.3f, -10.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        ModelPartData p_left_forearm = p_left_arm.addChild("left_forearm", ModelPartBuilder.create().uv(0, 24).cuboid(-1.8f, 0.0f, -1.8f, 3.6f, 6.0f, 3.6f).uv(104, 34).cuboid(-2.0f, 4.5f, -2.0f, 4.0f, 2.0f, 4.0f), ModelTransform.of(0.0f, 5.5f, 0.0f, 0.0f, 0.0f, 0.0f));
        ModelPartData p_shield_emitter = p_left_forearm.addChild("shield_emitter", ModelPartBuilder.create().uv(22, 34).cuboid(0.0f, -1.5f, -2.5f, 1.2f, 3.0f, 5.0f).uv(0, 43).cuboid(1.0f, -1.0f, -2.0f, 0.4f, 2.0f, 4.0f), ModelTransform.of(2.0f, 2.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        ModelPartData p_trigger_button = p_left_forearm.addChild("trigger_button", ModelPartBuilder.create().uv(34, 49).cuboid(-0.7f, 0.0f, -0.5f, 1.4f, 1.0f, 0.6f), ModelTransform.of(0.0f, 2.0f, -2.0f, 0.0f, 0.0f, 0.0f));
        ModelPartData p_right_arm = p_body.addChild("right_arm", ModelPartBuilder.create().uv(98, 13).cuboid(-2.0f, -2.0f, -2.0f, 4.0f, 6.0f, 4.0f).uv(0, 34).cuboid(-2.8f, -3.0f, -2.6f, 5.0f, 3.0f, 5.2f), ModelTransform.of(-6.3f, -10.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        ModelPartData p_right_forearm = p_right_arm.addChild("right_forearm", ModelPartBuilder.create().uv(16, 24).cuboid(-1.8f, 0.0f, -1.8f, 3.6f, 6.0f, 3.6f).uv(10, 43).cuboid(-2.0f, 4.5f, -2.0f, 4.0f, 2.0f, 4.0f), ModelTransform.of(0.0f, 5.5f, 0.0f, 0.0f, 0.0f, 0.0f));
        ModelPartData p_cannon = p_right_forearm.addChild("cannon", ModelPartBuilder.create().uv(12, 13).cuboid(-2.0f, -1.5f, -5.0f, 2.0f, 3.0f, 8.0f).uv(82, 43).cuboid(-1.6f, -0.6f, -7.0f, 1.2f, 1.2f, 2.0f).uv(74, 49).cuboid(-1.4f, -0.4f, -7.1f, 0.8f, 0.8f, 0.2f), ModelTransform.of(-2.0f, 2.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        return TexturedModelData.of(data, 128, 128);
    }
}
