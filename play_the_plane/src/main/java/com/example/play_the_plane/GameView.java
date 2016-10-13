package com.example.play_the_plane;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.SoundPool;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Created by WUYIXIONG on 2016-10-8.
 */

public class GameView extends SurfaceView implements Runnable, SurfaceHolder.Callback, View.OnTouchListener {

    private Bitmap bg;//背景
    private Bitmap bullet;//子弹
    private Bitmap enemy;//敌人
    private Bitmap explosion;//爆炸
    private Bitmap my;//自己
    private Bitmap erjihuancun;//二级缓存图片
    private int display_w;
    private int display_h;

    private int score = 0;//分数
    private int level = 1;//关卡数

    private int interval = 50;//出敌机的时间间隔
    private int speed = 3;//敌机的速度

    ArrayList<GameImage> list = new ArrayList<GameImage>();
    ArrayList<Zidan> bullets = new ArrayList<Zidan>();

    int[][] pass = {
            {1, 100, 23, 6},
            {2, 200, 21, 7},
            {3, 300, 19, 8},
            {4, 400, 17, 9},
            {5, 500, 15, 10},
            {6, 600, 13, 11},
            {7, 700, 11, 12},
            {8, 800, 9, 13},
            {9, 900, 7, 14},
            {10, 1000, 5, 15}};

    private SoundPool pool;//声音池
    private int sound_bomb = 0;
    private int sound_gameover = 0;
    private int sound_shot = 0;


    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        this.setOnTouchListener(this);//事件注册
    }

    private void init() {
        //加载图片
        bg = BitmapFactory.decodeResource(getResources(), R.drawable.background);
        bullet = BitmapFactory.decodeResource(getResources(), R.drawable.bullet);
        enemy = BitmapFactory.decodeResource(getResources(), R.drawable.enemy);
        explosion = BitmapFactory.decodeResource(getResources(), R.drawable.explosion);
        my = BitmapFactory.decodeResource(getResources(), R.drawable.my);
        //初始化二级缓存图片
        erjihuancun = Bitmap.createBitmap(display_w, display_h, Bitmap.Config.ARGB_8888);
        list.add(new BeiJingImage(bg));//先加入背景照片
        list.add(new FeiJiImage(my));//加入自己的飞机
        list.add(new DiJiImage(enemy, explosion));

        //加载声音
        pool = new SoundPool(10, AudioManager.STREAM_SYSTEM, 2);
        sound_bomb = pool.load(getContext(), R.raw.bomb, 1);
        sound_gameover = pool.load(getContext(), R.raw.gameover, 1);
        sound_shot = pool.load(getContext(), R.raw.shot, 1);
    }

    private boolean state = false;
    private SurfaceHolder holder = null;


    /**
     * 暂停
     */
    private boolean stop_state = false;

    public void stop() {
        stop_state = true;
    }

    /**
     * 取消暂停
     */
    public void start() {
        stop_state = false;
        mThread.interrupt();
    }

    /**
     * 播放声音的线程
     */
    public class PlaySound extends Thread{
        int id=0;
        public PlaySound(int i){
            this.id=i;
        }
        @Override
        public void run() {
            pool.play(id, 1, 1, 1, 0, 1);
        }
    }

    //绘画
    @Override
    public void run() {
        Paint p = new Paint();

        Paint p_score = new Paint();
        p_score.setColor(Color.YELLOW);
        p_score.setTextSize(30);
        p_score.setDither(true);
        p_score.setAntiAlias(true);

        int diji_num = 0;//出敌机的计时
        int bullet_num = 0;
        try {
            while (state) {
                try {
                    while (stop_state) {
                        Thread.sleep(10000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Canvas c = new Canvas(erjihuancun);
                //判断什么时候发射子弹
                if (selectFeiji != null) {
                    if (bullet_num == 7) {
                        bullets.add(new Zidan(selectFeiji, bullet));
                        new PlaySound(sound_shot).start();
                        bullet_num = 0;
                    }
                    bullet_num++;
                }


                for (GameImage image : (ArrayList<GameImage>) list.clone()) {
                    c.drawBitmap(image.getBitmap(), image.getX(), image.getY(), p);
                }

                for (Zidan image : (ArrayList<Zidan>) bullets.clone()) {
                    c.drawBitmap(image.getBitmap(), image.getX(), image.getY(), p);
                }

                if (score >= pass[level - 1][1]) {
                    interval = pass[level][2];
                    speed = pass[level][3];
                    score = score - pass[level - 1][1];
                    level++;
                }
                c.drawText("分数:" + score, 10f, 50f, p_score);
                c.drawText("关：" + level, 10f, 100f, p_score);

                //每当计数到40，出一架敌机
                if (diji_num >= interval) {
                    diji_num = 0;
                    list.add(new DiJiImage(enemy, explosion));
                }
                diji_num++;

                Canvas canvas = holder.lockCanvas();
                canvas.drawBitmap(erjihuancun, 0, 0, p);
                holder.unlockCanvasAndPost(canvas);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        this.holder = holder;
    }

    Thread mThread = new Thread(this);

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //获取屏幕的宽和高
        display_w = width;
        display_h = height;
        init();
        state = true;

        mThread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        state = false;
    }

    /**
     * 点击事件监听
     *
     * @param v
     * @param event
     * @return
     */
    FeiJiImage selectFeiji = null;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            for (GameImage image : list) {
                if (image instanceof FeiJiImage) {
                    FeiJiImage feiji = (FeiJiImage) image;
                    if (feiji.getX() < event.getX()
                            && feiji.getY() < event.getY()
                            && feiji.getX() + feiji.getWidth() > event.getX()
                            && feiji.getY() + feiji.getHeight() > event.getY()) {
                        selectFeiji = feiji;
                    } else {
                        selectFeiji = null;
                    }
                    break;
                }
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            selectFeiji = null;
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (selectFeiji != null) {
                selectFeiji.setX((int) (event.getX() - selectFeiji.getWidth() / 2));
                selectFeiji.setY((int) (event.getY() - selectFeiji.getHeight() / 2));
            }
        }
        return true;
    }



    private interface GameImage {
        public Bitmap getBitmap();

        public int getX();

        public int getY();
    }


    /**
     * 负责照片的处理
     */
    private class BeiJingImage implements GameImage {
        private Bitmap bg;
        private Bitmap newBitmap = null;

        public BeiJingImage(Bitmap bg) {
            this.bg = bg;
            newBitmap = Bitmap.createBitmap(display_w, display_h, Bitmap.Config.ARGB_8888);
        }

        private int height = 0;

        @Override
        public Bitmap getBitmap() {
            Canvas canvas = new Canvas(newBitmap);
            Paint p = new Paint();

            canvas.drawBitmap(bg,
                    new Rect(0, 0, bg.getWidth(), bg.getHeight()),
                    new Rect(0, height, display_w, display_h + height),
                    p);
            canvas.drawBitmap(bg,
                    new Rect(0, 0, bg.getWidth(), bg.getHeight()),
                    new Rect(0, -display_h + height, display_w, height),
                    p);
            height += 3;
            if (height >= display_h) {
                height = 0;
            }
            return newBitmap;
        }

        @Override
        public int getX() {
            return 0;
        }

        @Override
        public int getY() {
            return 0;
        }
    }


    /**
     * 飞机的封装类
     */
    private class FeiJiImage implements GameImage {

        private Bitmap my;
        List<Bitmap> bitmaps = new ArrayList<Bitmap>();
        private int x, y;
        private int width;//照片的宽
        private int height;//照片的高

        public FeiJiImage(Bitmap my) {
            this.my = my;
            bitmaps.add(Bitmap.createBitmap(my, 0, 0, my.getWidth() / 4, my.getHeight()));
            bitmaps.add(Bitmap.createBitmap(my, (my.getWidth() / 4) * 1, 0, my.getWidth() / 4, my.getHeight()));
            bitmaps.add(Bitmap.createBitmap(my, (my.getWidth() / 4) * 2, 0, my.getWidth() / 4, my.getHeight()));
            bitmaps.add(Bitmap.createBitmap(my, (my.getWidth() / 4) * 3, 0, my.getWidth() / 4, my.getHeight()));

            //得到照片的宽高
            width = my.getWidth() / 4;
            height = my.getHeight();
            //飞机的初始位置坐标
            x = (display_w - my.getWidth() / 4) / 2;
            y = display_h - my.getHeight() - 15;
        }

        private int index = 0;//飞机图片的下标
        private int num = 0;//控制图片更换的控制数，每调用10次 index加一

        @Override
        public Bitmap getBitmap() {
            Bitmap bitmap = bitmaps.get(index);
            if (num == 10) {
                index++;
                if (index == bitmaps.size()) {
                    index = 0;
                }
                num = 0;
            }
            num++;
            return bitmap;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public void setX(int x) {
            this.x = x;
        }

        public void setY(int y) {
            this.y = y;
        }

        @Override
        public int getX() {
            return x;
        }

        @Override
        public int getY() {
            return y;
        }

        public boolean select(int x, int y) {
            return false;
        }
    }

    /**
     * 敌人飞机的包装类
     */
    private class DiJiImage implements GameImage {
        Bitmap enemy;
        List<Bitmap> bitmaps = new ArrayList<Bitmap>();
        List<Bitmap> bitmaps_explosion = new ArrayList<Bitmap>();
        private int x;
        private int y;
        private int width;
        private int height;
        private boolean alive = true;//敌机是否活着

        public DiJiImage(Bitmap enemy, Bitmap explosion) {
            this.enemy = enemy;
            //拆分敌机的图片
            bitmaps.add(Bitmap.createBitmap(enemy,
                    0, 0, enemy.getWidth() / 4, enemy.getHeight()));
            bitmaps.add(Bitmap.createBitmap(enemy,
                    (enemy.getWidth() / 4) * 1, 0, enemy.getWidth() / 4, enemy.getHeight()));
            bitmaps.add(Bitmap.createBitmap(enemy,
                    (enemy.getWidth() / 4) * 2, 0, enemy.getWidth() / 4, enemy.getHeight()));
            bitmaps.add(Bitmap.createBitmap(enemy,
                    (enemy.getWidth() / 4) * 3, 0, enemy.getWidth() / 4, enemy.getHeight()));
            //拆分爆炸的图片
            bitmaps_explosion.add(Bitmap.createBitmap(explosion,
                    0, 0, explosion.getWidth() / 4, explosion.getHeight() / 2));
            bitmaps_explosion.add(Bitmap.createBitmap(explosion,
                    (explosion.getWidth() / 4) * 1, 0, explosion.getWidth() / 4, explosion.getHeight() / 2));
            bitmaps_explosion.add(Bitmap.createBitmap(explosion,
                    (explosion.getWidth() / 4) * 2, 0, explosion.getWidth() / 4, explosion.getHeight() / 2));
            bitmaps_explosion.add(Bitmap.createBitmap(explosion,
                    (explosion.getWidth() / 4) * 3, 0, explosion.getWidth() / 4, explosion.getHeight() / 2));

            bitmaps_explosion.add(Bitmap.createBitmap(explosion,
                    0, (explosion.getHeight() / 2), explosion.getWidth() / 4, explosion.getHeight() / 2));
            bitmaps_explosion.add(Bitmap.createBitmap(explosion,
                    (explosion.getWidth() / 4) * 1, (explosion.getHeight() / 2), explosion.getWidth() / 4, explosion.getHeight() / 2));
            bitmaps_explosion.add(Bitmap.createBitmap(explosion,
                    (explosion.getWidth() / 4) * 2, (explosion.getHeight() / 2), explosion.getWidth() / 4, explosion.getHeight() / 2));
            bitmaps_explosion.add(Bitmap.createBitmap(explosion,
                    (explosion.getWidth() / 4) * 3, (explosion.getHeight() / 2), explosion.getWidth() / 4, explosion.getHeight() / 2));


            y = -enemy.getHeight();
            Random ran = new Random();
            x = ran.nextInt(display_w - enemy.getWidth() / 4);

            width = enemy.getWidth() / 4;
            height = enemy.getHeight();
        }

        private int index = 0;//敌人飞机图片的下标
        private int num = 0;//控制图片更换的控制数，每调用10次 index加一

        @Override
        public Bitmap getBitmap() {
            Bitmap bitmap = bitmaps.get(index);
            if (num == 5) {
                index++;
                if (index == 8 && !alive) {
                    list.remove(this);
                }
                if (index == bitmaps.size()) {
                    index = 0;
                }
                num = 0;
            }
            num++;
            y += speed;
            if (y > display_h) {
                list.remove(this);
            }
            damage(bullets);
            return bitmap;
        }

        /**
         * 敌机被击中
         *
         * @param bullets 子弹
         */
        public void damage(ArrayList<Zidan> bullets) {
            if (alive) {
                for (Zidan bullet : bullets) {
                    if (bullet.getX() > getX()
                            && bullet.getY() > getY()
                            && bullet.getX() < getX() + width
                            && bullet.getY() < getY() + height) {
                        bullets.remove(bullet);
                        alive = false;
                        score += 10;
                        new PlaySound(sound_bomb).start();
                        bitmaps = bitmaps_explosion;
                        break;
                    }
                }
            }

        }

        @Override
        public int getX() {
            return x;
        }

        @Override
        public int getY() {
            return y;
        }
    }

    /**
     * 子弹的封装类
     */
    private class Zidan implements GameImage {
        FeiJiImage feiji;
        Bitmap bullet;
        private int x;
        private int y;

        public Zidan(FeiJiImage feiji, Bitmap bullet) {
            this.feiji = feiji;
            this.bullet = bullet;
            x = feiji.getX() + feiji.getWidth() / 2 - bullet.getWidth() / 2;
            y = feiji.getY() - bullet.getHeight();
        }

        @Override
        public Bitmap getBitmap() {
            y -= 25;
            if (y < -bullet.getHeight()) {
                bullets.remove(this);
            }
            return bullet;
        }

        @Override
        public int getX() {
            return x;
        }

        @Override
        public int getY() {
            return y;
        }
    }

}
