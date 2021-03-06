package com.feri.ninjarun.screen;

import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.feri.ninjarun.GameManager;
import com.feri.ninjarun.NinjaRun;
import com.feri.ninjarun.assets.AssetDescriptors;
import com.feri.ninjarun.assets.AssetPaths;
import com.feri.ninjarun.config.GameConfig;
import com.feri.ninjarun.ecs.system.AnimationModifierSystem;
import com.feri.ninjarun.ecs.system.BoundsSystem;
import com.feri.ninjarun.ecs.system.CameraMovementSystem;
import com.feri.ninjarun.ecs.system.CleanUpSystem;
import com.feri.ninjarun.ecs.system.CollisionSystem;
import com.feri.ninjarun.ecs.system.GravitySystem;
import com.feri.ninjarun.ecs.system.HUDRenderSystem;
import com.feri.ninjarun.ecs.system.MovementSystem;
import com.feri.ninjarun.ecs.system.RenderAnimationSystem;
import com.feri.ninjarun.ecs.system.RenderSystem;
import com.feri.ninjarun.ecs.system.ResetRunnerSystem;
import com.feri.ninjarun.ecs.system.ShurikenSpawnSystem;
import com.feri.ninjarun.ecs.system.SkierInputSystem;
import com.feri.ninjarun.ecs.system.WorldWrapSystem;
import com.feri.ninjarun.ecs.system.debug.DebugCameraSystem;
import com.feri.ninjarun.ecs.system.debug.DebugInputSystem;
import com.feri.ninjarun.ecs.system.debug.DebugRenderSystem;
import com.feri.ninjarun.ecs.system.debug.GridRenderSystem;
import com.feri.ninjarun.ecs.system.passive.EntityFactorySystem;
import com.feri.ninjarun.ecs.system.passive.SoundSystem;
import com.feri.ninjarun.ecs.system.passive.StartUpSystem;
import com.feri.ninjarun.ecs.system.passive.TiledSystem;
import com.feri.ninjarun.util.GdxUtils;

public class GameScreen extends ScreenAdapter implements InputProcessor {
    public static final Logger log = new Logger(GameScreen.class.getSimpleName(), Logger.DEBUG);

    private final AssetManager assetManager;
    private final SpriteBatch batch;
    private final NinjaRun game;

    private OrthographicCamera camera;
    private Viewport viewport;
    private Viewport hudViewport;
    private ShapeRenderer renderer;
    private PooledEngine engine; //main ECS class
    private BitmapFont font;
    private BitmapFont bigfont;
    //private boolean debug;
    TiledMap map1;


    /////////
    private Stage stage;
    private Skin skin;

    private Table table;
    public static TextButton retryButton;
    public static TextButton backButton;

    public static InputMultiplexer im;

    public GameScreen(NinjaRun game) {
        this.game = game;
        assetManager = game.getAssetManager();
        batch = game.getBatch();
        //debug = true;
    }

    @Override
    public void show() {
        //------Ui buttons ingame-------//
        skin = new Skin(Gdx.files.internal("uiskin.json"));
        stage = new Stage(new ScreenViewport());

        table = new Table();
        table.setWidth(stage.getWidth());
        table.align(Align.center| Align.top);

        table.setPosition(0, Gdx.graphics.getHeight());

        retryButton = new TextButton("Retry", skin);
        backButton = new TextButton("Back",skin);


        retryButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                //GameManager.INSTANCE.resetResult();
                GameConfig.restartGame = true;
            }
        });

        backButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.selectIntroScreen();
            }
        });

        table.padTop(30);
        table.add(retryButton).padBottom(10).padRight(40);
        table.add(backButton).padBottom(10).padLeft(40);

        stage.addActor(table);

        im = new InputMultiplexer(stage, this);
        Gdx.input.setInputProcessor(im);
        //--------//

        map1 = assetManager.get(AssetPaths.TILES1); //Rethink add with manager?

        camera = new OrthographicCamera();
        viewport = new FitViewport(GameConfig.WIDTH, GameConfig.HEIGHT, camera);
        hudViewport = new FitViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        renderer = new ShapeRenderer();
        font = assetManager.get(AssetDescriptors.FONT32);
        bigfont = assetManager.get(AssetDescriptors.FONT96);
        engine = new PooledEngine();
        engine.addSystem(new EntityFactorySystem(assetManager));
        engine.addSystem(new SoundSystem(assetManager));
        engine.addSystem(new TiledSystem(map1));

        engine.addSystem(new BoundsSystem());
        //engine.addSystem(new PlayerControlSystem());
        engine.addSystem(new WorldWrapSystem());
        engine.addSystem(new MovementSystem());
        engine.addSystem(new ShurikenSpawnSystem());
        engine.addSystem(new AnimationModifierSystem());
        engine.addSystem(new CollisionSystem());
        //-----
        engine.addSystem(new GravitySystem());
        engine.addSystem(new SkierInputSystem());
        engine.addSystem(new CameraMovementSystem());
        engine.addSystem(new ResetRunnerSystem());

        engine.addSystem(new RenderSystem(batch, viewport));
        engine.addSystem(new RenderAnimationSystem(batch,viewport));
        engine.addSystem(new StartUpSystem());
        engine.addSystem(new CleanUpSystem());
        engine.addSystem(new HUDRenderSystem(batch, hudViewport, font,bigfont));

        if (GameConfig.debug) {
            engine.addSystem(new GridRenderSystem(viewport, renderer));
            engine.addSystem(new DebugCameraSystem(
                    GameConfig.POSITION_X+ 70f*25, GameConfig.POSITION_Y+ 70f*12, //center
                    camera
            ));
            engine.addSystem(new DebugRenderSystem(viewport, renderer));
            engine.addSystem(new DebugInputSystem());
        }

        GameManager.INSTANCE.resetResult();
        printEngine();
    }

    int tmp = 0;
    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE)) Gdx.app.exit();
        if (Gdx.input.isKeyPressed(Input.Keys.R) || GameConfig.restartGame) { //resetam vse na zacetek
            GameManager.INSTANCE.resetResult();
            tmp++;
            if(tmp>=2){
                tmp = 0;
                GameConfig.restartGame = false;
            }
            //GameConfig.restartGame = false;
        }
        GdxUtils.clearScreen();
        if (GameManager.INSTANCE.isGameOver()){
            engine.update(0);}
        else{
            engine.update(delta);
            //log.debug("posx = " + GameConfig.POSITION_X);
        }


        stage.act(delta);
        stage.draw();

        // if (GameManager.INSTANCE.isGameOver()) {
        //     game.setScreen(new MenuScreen(game));
        // }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        hudViewport.update(width, height, true);
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        renderer.dispose();
        engine.removeAllEntities();
    }

    public void printEngine() {
        ImmutableArray<EntitySystem> systems =  engine.getSystems();
        for (EntitySystem system:systems) {
            System.out.println(system.getClass().getSimpleName());
        }
    }

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        return false;
    }
}