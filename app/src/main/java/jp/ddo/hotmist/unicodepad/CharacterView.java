package jp.ddo.hotmist.unicodepad;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.RectF;
import android.os.Build;
import android.support.v4.graphics.ColorUtils;
import android.util.AttributeSet;
import android.view.View;

public class CharacterView extends View
{
	private Paint paint = null;
	private Paint paintbg = null;
	private Paint paintsq = null;
	private Paint paintln = null;
	private String str;
	private int offsetx;
	private int offsety;
	private int ascent;
	private int descent;
	private float size = 0.f;
	private int width;
	private int height;
	private boolean shrinkWidth;
	private boolean shrinkHeight;
	private boolean drawSlash;
	private boolean validChar;

	private Bitmap emojicache;
	private boolean invalid;

	public CharacterView(Context context)
	{
		this(context, null);
	}

	public CharacterView(Context context, AttributeSet attrs)
	{
		this(context, attrs, 0);
	}

	public CharacterView(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);

		if (paint == null)
		{
			paint = new Paint();
			TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{android.R.attr.textColorPrimary});
			paint.setColor(a.getColor(0, Color.RED));
			a.recycle();
			paint.setStyle(Style.FILL);
			paint.setTypeface(Typeface.DEFAULT);
			paint.setAntiAlias(true);
			paint.setFilterBitmap(true);
		}
		str = "";
		offsetx = 0;
		offsety = 0;
		width = 0;
		height = 0;
		shrinkWidth = false;
		shrinkHeight = false;
		validChar = true;
		drawSlash = true;

		emojicache = null;
		invalid = true;
	}

	public void setText(String str)
	{
		if (!this.str.equals(str))
			invalid = true;

		this.str = str;

		requestLayout();
	}

	public String getText()
	{
		return str;
	}

	public void setTypeface(Typeface tf)
	{
		if (!(paint.getTypeface() == tf || paint.getTypeface() != null && paint.getTypeface().equals(tf)))
			invalid = true;

		paint.setTypeface(tf);

		requestLayout();
	}

	public void setTextSize(float size)
	{
		if (this.size != getContext().getResources().getDisplayMetrics().scaledDensity * size)
			invalid = true;

		this.size = getContext().getResources().getDisplayMetrics().scaledDensity * size;

		requestLayout();
	}

	public void setTextColor(int color)
	{
		if (paint.getColor() != color)
			invalid = true;

		paint.setColor(color);
		if (paintsq != null)
			paintsq.setColor(ColorUtils.setAlphaComponent(color, paintsq.getColor() / 0x1000000));
	}

	public void setBackgroundColor(int color)
	{
		if (color == Color.TRANSPARENT)
			paintbg = null;
		else
		{
			if (paintbg == null)
			{
				paintbg = new Paint();
				paintbg.setStyle(Style.FILL);
			}
			paintbg.setColor(color);
		}
	}

	public void setSquareAlpha(int alpha)
	{
		if (alpha == 0)
			paintsq = null;
		else
		{
			if (paintsq == null)
			{
				paintsq = new Paint();
				paintsq.setStyle(Style.FILL);
			}
			paintsq.setColor(ColorUtils.setAlphaComponent(paint.getColor(), alpha));
		}
	}

	public void setValid(boolean valid)
	{
		validChar = valid;
	}

	public void drawLines(boolean draw)
	{
		if (!draw)
			paintln = null;
		else
		{
			if (paintln == null)
			{
				paintln = new Paint();
				paintln.setColor(Color.RED);
			}
		}
	}

	public void shrinkWidth(boolean shrink)
	{
		shrinkWidth = shrink;

		requestLayout();
	}

	public void shrinkHeight(boolean shrink)
	{
		shrinkHeight = shrink;

		requestLayout();
	}

	public void drawSlash(boolean draw)
	{
		drawSlash = draw;

		requestLayout();
	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);

		int heightNew, heightOld = 0;
		float sizeu = size, sizeb = 0.f, size_ = size;
		paint.setTextSize(size_);
		FontMetrics fm;
		while (true)
		{
			size_ = (sizeu + sizeb) / 2.f;
			paint.setTextSize(size_);
			fm = paint.getFontMetrics();
			heightNew = (int)(-fm.top + fm.bottom);
			if (heightNew > 0)
			{
				if (heightNew == heightOld)
					break;
				heightOld = heightNew;
				sizeb = size_;
			}
			else
				sizeu = size_;
		}
		heightNew += getPaddingTop() + getPaddingBottom();

		boolean measure = android.support.v4.graphics.PaintCompat.hasGlyph(paint, str) && validChar || !drawSlash;

		int widthNew;
		widthNew = (int)(measure ? paint.measureText(str) : fm.descent - fm.ascent) + getPaddingLeft() + getPaddingRight();

		if (widthMode != MeasureSpec.EXACTLY)
			if (widthMode == MeasureSpec.UNSPECIFIED || widthSize > widthNew)
				widthSize = widthNew;

		if (!shrinkHeight)
			if (heightMode != MeasureSpec.EXACTLY)
				if (heightMode == MeasureSpec.UNSPECIFIED || heightSize > heightNew)
					heightSize = heightNew;

		if (shrinkWidth)
			if (widthNew > widthSize)
			{
				size_ = size_ * widthSize / (widthNew - getPaddingLeft() - getPaddingRight());
				paint.setTextSize(size_);
				fm = paint.getFontMetrics();
				widthNew = (int)(measure ? paint.measureText(str) : fm.descent - fm.ascent) + getPaddingLeft() + getPaddingRight();
				heightNew = (int)(-fm.top + fm.bottom);
				heightNew += getPaddingTop() + getPaddingBottom();
			}

		if (shrinkHeight)
			if (heightMode != MeasureSpec.EXACTLY)
				if (heightMode == MeasureSpec.UNSPECIFIED || heightSize > heightNew)
					heightSize = heightNew;

		offsetx = (widthSize - widthNew) / 2 + getPaddingLeft();
		ascent = (int)((heightSize - heightNew) / 2 - fm.top + fm.ascent) + getPaddingTop();
		offsety = (int)((heightSize - heightNew) / 2 - fm.top) + getPaddingTop();
		descent = (int)((heightSize - heightNew) / 2 - fm.top + fm.descent) + getPaddingTop();

		width = widthSize;
		height = heightSize;

		setMeasuredDimension(widthSize, heightSize);
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);

		if (paintbg != null)
			canvas.drawPaint(paintbg);

		if (paintsq != null)
			for (int j = (int)Math.floor(-width * 6. / height); j < (int)Math.ceil(width * 6. / height); ++j)
				for (int i = 0; i < 12; ++i)
					if ((i + j) % 2 == 0)
						canvas.drawRect(width / 2 + height * j / 12, height * i / 12, width / 2 + height * (j + 1) / 12, height * (i + 1) / 12, paintsq);

		if (paintln != null)
		{
			canvas.drawLine(0, ascent, width, ascent, paintln);
			canvas.drawLine(0, offsety, width, offsety, paintln);
			canvas.drawLine(0, descent, width, descent, paintln);
			if (android.support.v4.graphics.PaintCompat.hasGlyph(paint, str) && validChar || !drawSlash)
			{
				canvas.drawLine(offsetx, 0, offsetx, height, paintln);
				canvas.drawLine(width - offsetx, 0, width - offsetx, height, paintln);
			}
		}

		if (str.length() > 0)
		{
			if (android.support.v4.graphics.PaintCompat.hasGlyph(paint, str) && validChar || !drawSlash)
			{
				if (invalid)
					getCache();

				if (emojicache == null)
					canvas.drawText(str, offsetx, offsety, paint);
				else
					canvas.drawBitmap(emojicache, new Rect(0, 0, emojicache.getWidth(), emojicache.getHeight()), new RectF(offsetx, getPaddingTop(), width - offsetx + getPaddingLeft() - getPaddingRight(), height - getPaddingBottom()), paint);
			}
			else
			{
				float sz = descent - ascent;
				canvas.drawLine(width / 2 - sz * .4f, ascent + sz * .1f, width / 2 - sz * .4f, ascent + sz * .9f, paint);
				canvas.drawLine(width / 2 - sz * .4f, ascent + sz * .1f, width / 2 + sz * .4f, ascent + sz * .1f, paint);
				canvas.drawLine(width / 2 + sz * .4f, ascent + sz * .1f, width / 2 + sz * .4f, ascent + sz * .9f, paint);
				canvas.drawLine(width / 2 - sz * .4f, ascent + sz * .9f, width / 2 + sz * .4f, ascent + sz * .9f, paint);
				if (!android.support.v4.graphics.PaintCompat.hasGlyph(paint, str))
					canvas.drawLine(width / 2 - sz * .4f, ascent + sz * .1f, width / 2 + sz * .4f, ascent + sz * .9f, paint);
				if (!validChar)
					canvas.drawLine(width / 2 + sz * .4f, ascent + sz * .1f, width / 2 - sz * .4f, ascent + sz * .9f, paint);
			}
		}
	}

	private void getCache()
	{
		emojicache = null;

		if (Build.VERSION.SDK_INT >= 12 && android.support.v4.graphics.PaintCompat.hasGlyph(paint, str) && validChar && paint.measureText(str) > 0.f)
		{
			float w = paint.measureText(str);
			if (w > 256.f)
			{
				float size_ = paint.getTextSize();
				paint.setTextSize(size_ * 256.f / w);
				FontMetrics fm = paint.getFontMetrics();
				Bitmap bm = Bitmap.createBitmap((int)paint.measureText(str), (int)(-fm.top + fm.bottom), Bitmap.Config.ARGB_8888);
				Bitmap tr = Bitmap.createBitmap((int)paint.measureText(str), (int)(-fm.top + fm.bottom), Bitmap.Config.ARGB_8888);
				Canvas cv = new Canvas(bm);
				cv.drawText(str, 0, -fm.top, paint);
				if (!bm.sameAs(tr))
				{
					cv.drawColor(0, PorterDuff.Mode.CLEAR);
					paint.setStyle(Style.STROKE);
					cv.drawText(str, 0, -fm.top, paint);
					if (bm.sameAs(tr))
					{
						paint.setStyle(Style.FILL);
						cv.drawText(str, 0, -fm.top, paint);
						emojicache = bm;
					}
					else
						paint.setStyle(Style.FILL);
				}
				paint.setTextSize(size_);
			}
		}

		invalid = false;
	}
}
