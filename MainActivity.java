package com.example.musicplayer;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class MainActivity extends Activity implements OnGestureListener {

	/* 实例化Random对象.用于产生随即数,实现播放界面图片的随即显示,用于随即音乐播放 */
	Random random = new Random();

	/* 图片库 图片数组.用于播放音乐主界面的ImageButton图片显示库 */
	final static int[] IMAGES = { R.drawable.image1, R.drawable.image2,
			R.drawable.image3, R.drawable.image4, R.drawable.image5,
			R.drawable.image6, R.drawable.image7, R.drawable.image8,
			R.drawable.image9 };

	/* 声明音乐播放seekBar父控件RelativeLayout对象,控制声音SeekBar和音乐播放进度SeekBar的显示与消失 */
	View musicPragrass;

	/* Menu菜单中,设置布局对象 */
	View dialogView;

	/* 音乐播放进度中 用于显示播放了多少时间的textView */
	TextView playTime;

	/* 音乐播放进度中,用于显示歌曲总时间的TexTView */
	TextView musicTime;

	/* Menu菜单中,设置, 传感器关闭与否的checkBox */
	CheckBox issensor;

	/* Menu菜单中,设置, 随即播放与否的checkBox */
	CheckBox isRandom;

	/* MediaPlayer 多媒体播放类,此处只用到音乐播放 */
	public MediaPlayer mMediaPlayer;

	/* 音乐文件列表 数据存储源 存放的是音乐文件名 */
	private List<String> mMusicList = new ArrayList<String>();

	/* 表示当前播放的音乐在 音乐文件 List<String> mMusicList 中的下标位置 */
	private int currentListItem = -1;

	/* 音乐文件存放目录 */
	private static final String MUSIC_PATH = new String("/mnt/sdcard/");//music/alarms/

	/* 控制SeekBar的按钮 */
	private ImageView volume_control;

	/* 用于显示音乐列表的空间 ListView */
	private ListView musicListView;

	ViewFlipper vf;

	GestureDetector gd;

	private ImageView front, pause, play, next, setting , record;

	/* 音乐播放界面 中间图片空间 点击是可以实现 隐藏与显示 音量控制SeekBar和音乐播放进度SeekBar */
	private ImageButton image_music;

	/* listView 页面 title */
	Button titleButton;

	/* 音乐播放页面title */
	TextView namePlay;

	/* 音乐播放进度的SeekBar */
	SeekBar seekbar_music;

	/* seekBar 音量控制,音乐进度控制，定义常量,当seekBar 中RROGRESS改变时的ID */
	static final int PROGRESS_CHANGED = 0x101;

	/* 音量控制SeekBar */
	SeekBar volume;

	/* 声音管理 类 */
	AudioManager mAudioManager;

	/* 声明 int类型变量 分别表示声音最大音量,声音当前的音量 */
	private int maxVolume, currentVolume;

	/* 设置声音音量的方法(自己定义的) */
	private void setVolume() {
		/* 通过AudioManager获得当前播放的音乐的最大音量 */
		maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		/* 设置音量控制SeekBar的最大值 */
		volume.setMax(maxVolume);
		/* 通过AudioManager获得当前播放音乐的当前音量 */
		currentVolume = mAudioManager
				.getStreamVolume(AudioManager.STREAM_MUSIC);
		/* 设置音量控制SeekBar游动标的当前值 */
		volume.setProgress(currentVolume);
	}

	/* 建立声音音量控制类,线程实现 */
	class myVolThread implements Runnable {

		public void run() {

			while (!Thread.currentThread().isInterrupted()) {
				/* 实例换Message对象 */
				Message message = new Message();
				/* 使用message对象传递我们要使用的ID */
				message.what = PROGRESS_CHANGED;
				/* 使用Handler对象myHandler传递message */
				MainActivity.this.myHandler.sendMessage(message);
				try {
					/* 线程睡眠0.1s */
					Thread.sleep(100);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/* 根据布局文件创建界面Activity显示 */
		setContentView(R.layout.combine);
		
		/* 实例化MediaPlayer类 */
		mMediaPlayer = new MediaPlayer();

		/* 实例化部分控件 */
		dosomething();

		/* 实现播放进度的显示与隐藏,图片的随机显示 */
		volume_control.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				if (volume.getVisibility() == View.VISIBLE) {
					/* 设置音量控制SeekBar不可见 */
					volume.setVisibility(View.INVISIBLE);
				} else if (volume.getVisibility() == View.INVISIBLE) {
					/* 设置音量控制SeekBar可见 */
					volume.setVisibility(View.VISIBLE);
				}
			}
		});

		/* 建立Click监听器,在点击image_music时 */
		image_music.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				/* 当音量控制SeekBar和 音乐播放进度SeekBar都是可见的时候 */
				if (musicPragrass.getVisibility() == View.VISIBLE) {
					/* 设置音量控制SeekBar不可见 */
					/* volume.setVisibility(View.INVISIBLE); */
					/* 设置音乐播放进度SeekBar不可见 */
					musicPragrass.setVisibility(View.INVISIBLE);
				} else
				/* 当音量控制SeekBar和 音乐播放进度SeekBar都是不可见的时候 */
				if (musicPragrass.getVisibility() == View.INVISIBLE) {
					/* 设置音量控制SeekBar可见 */
					/* volume.setVisibility(View.VISIBLE); */
					/* 设置音乐播放进度SeekBar不可见 */
					musicPragrass.setVisibility(View.VISIBLE);
				}
			}
		});

		/* seekBar 音量控制,音乐进度控制 , 建立音量控制SeekBar监听器..监听SeekBar的改变 */
		volume.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			public void onStopTrackingTouch(SeekBar seekBar) {

			}

			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				/* 将改变的值通过mAudioManager 设置为声音的改变 */
				mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
						progress, 0);
			}
		});

		/* 开始一个音量控制的线程,新的线程,使用的是音量控制的自定义线程类 */
		new Thread(new myVolThread()).start();

		/* 音乐进度控制,建立音乐播放控制SeekBar监听器..监听SeekBar的改变 */
		seekbar_music.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			public void onStopTrackingTouch(SeekBar seekBar) {

			}

			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				/* 判断,此音乐控制播放SeekBar的改变是因为使用者改变的,而非自动变化 */
				if (fromUser == true) {
					/*
					 * 通多媒体类对象mMeidaPlayer,再根据使用者改变SeekBar时传回的值progress来设置音乐播放的位置
					 */
					mMediaPlayer.seekTo(progress);
				}
				/* 获得音乐播放的当前时间和当前播放音乐的总时间长度 */
				getMusicTime(progress);
			}
		});

		/* 建立音乐显示列表ListView 适配器Adapter的方法 */
		musicList();

		/* 建立音乐显示列表ListView 对象musicListView 的item点击事件监听器 */
		musicListView.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				/* 获得点击的ListView的下标,同时也是音乐文件(文件名)存放的list的下标,以此获得要播放的音乐 */
				currentListItem = position;
				/* 音乐列表显示界面的title */
				titleButton.setText(mMusicList.get(position));
				/* 显示音乐播放的界面 */
				MainActivity.this.vf.setInAnimation(AnimationUtils
						.loadAnimation(MainActivity.this, R.anim.push_left_in));
				MainActivity.this.vf.setOutAnimation(AnimationUtils
						.loadAnimation(MainActivity.this, R.anim.push_left_out));
				vf.showNext();

				/* 播放选中的音乐 */
				playMusic(MUSIC_PATH + mMusicList.get(position));

				/* 音乐播放界面的title */
				namePlay.setText(mMusicList.get(position));

				/* 随即显示图片 */
				image_music.setImageDrawable(getResources().getDrawable(
						IMAGES[random.nextInt(9)]));
			}
		});

		/* titleButton 音乐播放列表下title按键,点击跳转到音乐播放主页面,建立音乐播放界面title (textView)的监听器 */
		titleButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				/* ViewFlipper 显示下一个页面 */
				MainActivity.this.vf.setInAnimation(AnimationUtils
						.loadAnimation(MainActivity.this, R.anim.push_left_in));
				MainActivity.this.vf.setOutAnimation(AnimationUtils
						.loadAnimation(MainActivity.this, R.anim.push_left_out));
				vf.showNext();
			}
		});

		/* namePlay 音乐播放页面下title TextView,点击跳转到音乐列表 */
		namePlay.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				/* ViewFlipper 显示下一个页面 */
				MainActivity.this.vf.setInAnimation(AnimationUtils
						.loadAnimation(MainActivity.this, R.anim.push_left_in));
				MainActivity.this.vf.setOutAnimation(AnimationUtils
						.loadAnimation(MainActivity.this, R.anim.push_left_out));
				vf.showNext();
			}
		});

		/* 播放控制 */
		front.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {

				FrontMusic();

				/* 随即显示图片 */
				image_music.setImageDrawable(getResources().getDrawable(
						IMAGES[random.nextInt(9)]));
			}
		});

		pause.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				try {
					if (currentListItem == -1) {
						currentListItem = 0;

						playMusic(MUSIC_PATH + mMusicList.get(currentListItem));

						titleButton.setText(mMusicList.get(currentListItem));
						namePlay.setText(mMusicList.get(currentListItem));
					} else {
						if (mMediaPlayer.isPlaying()) {
							mMediaPlayer.pause();
						} else {
							mMediaPlayer.start();
						}
					}
				} catch (IllegalStateException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		play.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				try {
					if (currentListItem == -1) {
						currentListItem = 0;

						playMusic(MUSIC_PATH + mMusicList.get(currentListItem));
						mMediaPlayer.setLooping(true);
						titleButton.setText(mMusicList.get(currentListItem));
						namePlay.setText(mMusicList.get(currentListItem));
					} else {
						if (mMediaPlayer.isPlaying()) {
							mMediaPlayer.pause();
						} else {
							mMediaPlayer.start();
							namePlay.setText(mMusicList.get(currentListItem));
						}
					}
				} catch (IllegalStateException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		next.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				image_music.setImageDrawable(getResources().getDrawable(
						IMAGES[random.nextInt(9)]));
				nextMusic();
			}
		});
		
		setting.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this,Setting.class);
				startActivity(intent);
			}
		});
		
		record.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this,Record.class);
				startActivity(intent);
			}
		});
	}

	/* 音乐播放控制 */
	Handler handler = new Handler();

	Runnable updateThread = new Runnable() {
		public void run() {
			/* 获得歌曲现在播放位置并设置成播放进度条的值 */
			seekbar_music.setProgress(mMediaPlayer.getCurrentPosition());
			/* 每次延迟100毫秒再启动线程 */
			handler.postDelayed(updateThread, 100);
		}
	};

	/* 音量播放控制 */
	Handler myHandler = new Handler() {

		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case PROGRESS_CHANGED:
				setVolume();
				break;
			default:
				break;
			}
		};
	};

	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_BACK) {
			new AlertDialog.Builder(MainActivity.this)
					.setTitle("退出")
					.setMessage("确定要退出??")
					.setPositiveButton("确定",
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int which) {
									try {
										handler.removeCallbacks(updateThread);
										mMediaPlayer.stop();
										mMediaPlayer.release();
										MainActivity.this.finish();
									} catch (IllegalStateException e) {
										e.printStackTrace();
									}
								}
							}).setNegativeButton("取消", null).show();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	/* 音乐随机播放控制 */
	void nextRandom() {
		try {
			handler.removeCallbacks(updateThread);

			playMusic(MUSIC_PATH
					+ mMusicList.get(random.nextInt(mMusicList.size())));

			namePlay.setText(mMusicList.get(random.nextInt(mMusicList.size())));

			titleButton
					.setText(mMusicList.get(random.nextInt(mMusicList.size())));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/* 音乐播放控制方法的具体实现 */
	protected void FrontMusic() {
		try {
			currentListItem = currentListItem - 1;
			if (currentListItem >= 0) {
				playMusic(MUSIC_PATH + mMusicList.get(currentListItem));

				namePlay.setText(mMusicList.get(currentListItem));
				titleButton.setText(mMusicList.get(currentListItem));
			} else {
				handler.removeCallbacks(updateThread);
				Toast.makeText(MainActivity.this, "已经是第一首歌曲了",
						Toast.LENGTH_SHORT).show();
				currentListItem = 0;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void nextMusic() {
		try {
			currentListItem = currentListItem + 1;
			if (currentListItem >= mMusicList.size()) {
				Toast.makeText(MainActivity.this, "已经是最后一首曲了",
						Toast.LENGTH_SHORT).show();
				currentListItem = mMusicList.size() - 1;
			} else {
				handler.removeCallbacks(updateThread);
				playMusic(MUSIC_PATH + mMusicList.get(currentListItem));
				namePlay.setText(mMusicList.get(currentListItem));
				titleButton.setText(mMusicList.get(currentListItem));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/* 播放音乐的具体实现 */
	protected void playMusic(String string) {
		try {
			mMediaPlayer.reset();

			mMediaPlayer.setDataSource(string);

			mMediaPlayer.prepare();

			mMediaPlayer.start();

			/* 每次播放音乐时对音乐播放控制SeekBar设定最大值,为要播放音乐的事件长度 */
			seekbar_music.setMax(mMediaPlayer.getDuration());

			/* 为音乐播放控制SeekBar 开始一个控制线程 */
			handler.post(updateThread);

			mMediaPlayer.setOnCompletionListener(new OnCompletionListener() {

				public void onCompletion(MediaPlayer mp) {
					nextMusic();
				}
			});
		} catch (Exception e) {
		}
	}

	/* 实例化控件 */
	private void dosomething() {
		/* 声音控制键 */
		volume_control = (ImageView) findViewById(R.id.volume_control);

		/* 手势实现viewFlipper */
		vf = (ViewFlipper) findViewById(R.id.ViewFlipper01);

		musicListView = (ListView) findViewById(R.id.musicList);

		/* 实例化声音管理类AudioManager对象mAudioManager */
		mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

		/* 实例化 音乐播放界面 中心图片显示空间ImageButton image_music */
		image_music = (ImageButton)findViewById(R.id.imagebutton01_layout02);

		/* 音量控制,实例化音量控制SeekBar */
		volume = (SeekBar) findViewById(R.id.seekbar_sound);

		/* 音乐控制 ,实例化承载 音乐播放进度控制SeekBar与播放时间 空间的布局对象 */
		musicPragrass = (View) findViewById(R.id.music_progress);

		/* 实例化音乐播放进度控制SeekBar */
		seekbar_music = (SeekBar) findViewById(R.id.seekbar_music);

		/* 实例化当前播放音乐播放时间TextVie */
		playTime = (TextView) findViewById(R.id.playtime);
		/* 实例换当前播放音乐总时间长度显示TextView */
		musicTime = (TextView) findViewById(R.id.musictime);

		/* Menu 中 设置 的布局对象 */
		LayoutInflater inflater = getLayoutInflater();

		dialogView = inflater.inflate(R.layout.setting_dialog,
				(ViewGroup) findViewById(R.layout.setting_dialog));

		/* 实例化 Menu 中 设置 布局对象中 音乐随即播放 checkBox */
		isRandom = (CheckBox) dialogView.findViewById(R.id.isRandom);

		/* 音乐播放列表下title按键,点击跳转到音乐播放主页面 */
		titleButton = (Button) findViewById(R.id.title);

		/* 实例化播放控制按钮 */
		front = (ImageView) findViewById(R.id.front);
		pause = (ImageView) findViewById(R.id.pause);
		play = (ImageView) findViewById(R.id.play);
		next = (ImageView) findViewById(R.id.next);
		setting = (ImageView) findViewById(R.id.setting);
		record = (ImageView) findViewById(R.id.record);

		/* 音乐播放界面的title */
		namePlay = (TextView) findViewById(R.id.textView01_layout02);
	}

	/* 建立musicList 列表 */
	private void musicList() {
		try {
			File home = new File(MUSIC_PATH);
			if (home.listFiles(new MusicFilter()).length > 0) {
				for (File file : home.listFiles(new MusicFilter())) {
					mMusicList.add(file.getName());
				}

				ArrayAdapter<String> adapter = new ArrayAdapter<String>(
						MainActivity.this, R.layout.item_list_music, mMusicList);
				musicListView.setAdapter(adapter);
			}
		} catch (NullPointerException e) {
			Toast.makeText(MainActivity.this, R.string.erro_notfindmusicfile,
					Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return gd.onTouchEvent(event);
	}

	public boolean onDown(MotionEvent e) {
		return false;
	}

	public void onShowPress(MotionEvent e) {

	}

	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		return false;
	}

	public void onLongPress(MotionEvent e) {

	}

	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {

		if (e1.getX() - e2.getX() > 120) {
			this.vf.setInAnimation(AnimationUtils.loadAnimation(this,
					R.anim.push_left_in));
			this.vf.setOutAnimation(AnimationUtils.loadAnimation(this,
					R.anim.push_left_out));
			this.vf.showNext();
		} else if (e2.getX() - e1.getX() > 120) {
			this.vf.setInAnimation(AnimationUtils.loadAnimation(this,
					R.anim.push_right_in));
			this.vf.setOutAnimation(AnimationUtils.loadAnimation(this,
					R.anim.push_right_out));
			this.vf.showPrevious();
		}
		return false;
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	/* 获取音乐时间 */
	void getMusicTime(int currentTime) {
		int music_time = mMediaPlayer.getDuration();
		int second = ((int) (music_time / 1000)) % 60;
		int min = (int) ((music_time / 1000 - second) / 60);
		musicTime.setText(min + ":" + second);
		int play_second = ((int) (currentTime / 1000)) % 60;
		int play_min = (int) ((currentTime / 1000 - second) / 60);
		playTime.setText(play_min + ":" + play_second);
	}

	/* 菜单 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, "返回");
		menu.add(0, 1, 1, "设置");
		menu.add(0, 2, 2, "关于");
		menu.add(0, 3, 3, "更新");
		menu.add(0, 4, 4, "退出");
		menu.add(0, 5, 5, "更多");
		return true;
	}

	/* 添加菜单 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
		case 0:
			Intent intent = new Intent(MainActivity.this, MainActivity.class);
			startActivity(intent);
			break;
		case 1:
			final Builder dialog_setting = new AlertDialog.Builder(
					MainActivity.this);
			dialog_setting.setTitle("设置");
			dialog_setting.setView(dialogView);
			dialog_setting.setPositiveButton("确定",
					new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							if (isRandom.isChecked()) {
								nextRandom();
							}
							dialog.cancel();
						}
					});
			dialog_setting.setNegativeButton("取消", null);
			dialog_setting.show();
			break;
		case 2:
			new AlertDialog.Builder(MainActivity.this)
					.setTitle("关于")
					.setMessage("版本:1.0.0")
					.setPositiveButton("确定",
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int which) {
									dialog.cancel();
								}
							}).show();
			break;
		case 3:
			new AlertDialog.Builder(MainActivity.this)
					.setTitle("更新")
					.setMessage("请确定更新?")
					.setPositiveButton("确定",
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int which) {
									dialog.cancel();
								}
							}).show();
			break;
		case 4:
			new AlertDialog.Builder(MainActivity.this)
					.setTitle("退出")
					.setMessage("您确定要退出?")
					.setPositiveButton("确定",
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int which) {
									try {
										handler.removeCallbacks(updateThread);
										mMediaPlayer.stop();
										mMediaPlayer.release();
										MainActivity.this.finish();
									} catch (IllegalStateException e) {
										e.printStackTrace();
									}
								}
							}).setNegativeButton("取消", null).show();

			break;
		case 5:
			new AlertDialog.Builder(MainActivity.this)
					.setTitle("更多")
					.setMessage("更多信息...")
					.setPositiveButton("确定",
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int which) {
									dialog.cancel();
								}
							}).show();
			break;
		}
		return true;
	}

}

/* 音乐文件过滤器 过滤mp3格式的音乐文件 */
class MusicFilter implements FilenameFilter {
	public boolean accept(File dir, String filename) {

		if ((filename.endsWith(".mp3"))) {
			return true;
		} else {
			return false;
		}
	}
}



