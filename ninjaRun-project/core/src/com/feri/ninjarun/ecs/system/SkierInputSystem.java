package com.feri.ninjarun.ecs.system;


import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.feri.ninjarun.GameManager;
import com.feri.ninjarun.config.GameConfig;
import com.feri.ninjarun.ecs.component.DimensionComponent;
import com.feri.ninjarun.ecs.component.Mappers;
import com.feri.ninjarun.ecs.component.MovementComponentXYR;
import com.feri.ninjarun.ecs.component.SkierComponent;
import com.feri.ninjarun.ecs.component.TransformComponent;


public class SkierInputSystem extends IteratingSystem {

    private static final Family FAMILY = Family.all(
            SkierComponent.class,
            MovementComponentXYR.class,
            TransformComponent.class,
            DimensionComponent.class
    ).get();

    public SkierInputSystem() {
        super(FAMILY);
    }

    // we don't need to override update Iterating system method because there is no batch.begin/end

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        MovementComponentXYR movement = Mappers.MOVEMENT.get(entity);
        TransformComponent transform = Mappers.TRANSFORM.get(entity);
        DimensionComponent dimension = Mappers.DIMENSION.get(entity);

        //movement.xSpeed = 0;
        transform.newHeightMultiplier = 1f;
        dimension.height = GameConfig.SKIER_HEIGHT;

        movement.xSpeed = GameConfig.MAX_SKIER_X_SPEED * deltaTime;

        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            movement.xSpeed = GameConfig.MAX_SKIER_X_SPEED*2 * deltaTime;
        } else if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            movement.xSpeed = -GameConfig.MAX_SKIER_X_SPEED * deltaTime;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            if (GameManager.INSTANCE.getJumpCounter() < 2) {
                movement.ySpeed = GameConfig.JUMP_SPEED * deltaTime;
                GameManager.INSTANCE.incJumpCounter();
            }
        }else if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            transform.newHeightMultiplier = GameConfig.SKIER_HEIGHT_TRANSFORM_MULTIPLIER;
            dimension.height = GameConfig.SKIER_HEIGHT * transform.newHeightMultiplier;
        }

    }
}
