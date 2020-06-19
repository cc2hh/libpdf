package com.sclbxx.libpdf.scrollhandle

import android.app.Dialog
import android.content.Context
import android.graphics.Paint
import android.os.Handler
import android.text.*
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.scroll.ScrollHandle
import com.github.barteksc.pdfviewer.util.Util
import com.sclbxx.libpdf.R
import com.sclbxx.libpdf.util.MyIMM
import com.sclbxx.libpdf.util.SystemUtil

/**
 * @title: WpsScrollHandle
 * @projectName FamiliesSchoolConnection
 * @description: wps样式的快速导航栏
 * @author cc
 * @date 2020/6/12 10:59
 */
class WpsScrollHandle : LinearLayout, ScrollHandle {
    private var pdfView: PDFView? = null
    private val tvCurrent: TextView
    private val tvAll: TextView

    constructor(ctx: Context) : this(ctx, null)
    constructor(ctx: Context, attributeSet: AttributeSet?) : super(ctx, attributeSet) {
        // 默认隐藏，
        visibility = View.GONE
        val view = LayoutInflater.from(context).inflate(R.layout.libpdf_handle_wps, this, true)
        tvCurrent = view.findViewById(R.id.tv_handle_wps_current)
        tvAll = view.findViewById(R.id.tv_handle_wps_all)

        tvCurrent.paintFlags = Paint.UNDERLINE_TEXT_FLAG

        setOnClickListener {
            val dialog = Dialog(ctx)
            dialog.setContentView(R.layout.libpdf_dialog_handle)
            val et = dialog.findViewById<EditText>(R.id.et_dialog_handle)
            val btnOk = dialog.findViewById<Button>(R.id.btn_dialog_handle_ok)
            et.hint = "(1-${pdfView?.pageCount})"
            btnOk.isEnabled = false

            et.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val v = s.toString()
                    btnOk.isEnabled = if (TextUtils.isEmpty(v)) {
                        false
                    } else {
                        if (v.toInt() <= 0) {
                            et.setText("1")
                            et.selectAll()
                        } else if (v.toInt() > pdfView!!.pageCount) {
                            et.setText("${pdfView?.pageCount}")
                            et.selectAll()
                        }
                        true
                    }
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }
            })

            dialog.findViewById<Button>(R.id.btn_dialog_handle_cancel).setOnClickListener {
                dialog.dismiss()
            }

            btnOk.setOnClickListener {
                // 页码从1开始，取值从0开始
                pdfView?.jumpTo(et.text.toString().toInt() - 1)
                hideDelayed()
                dialog.dismiss()
            }
            dialog.setOnDismissListener {
                MyIMM.hideSoftInput(ctx, et)
            }
            dialog.show()
            val p = dialog.window?.attributes
//            p.height = (SystemUtil.getScreenSize(ctx).y*0.8f).toInt()
            p?.width = (SystemUtil.getScreenSize(ctx).x * 0.6f).toInt()
            dialog.window?.attributes = p

            Handler().postDelayed({
                et.isFocusable = true
                et.isFocusableInTouchMode = true
                //请求获得焦点
                et.requestFocus()
                MyIMM.ShowKeyboard(ctx, et)
            }, 200)
        }
    }

    private val hidePageScrollerRunnable = Runnable { hide() }

    override fun setPageNum(pageNum: Int) {
        tvCurrent.text = "$pageNum"
    }

    override fun destroyLayout() {
        pdfView?.removeView(this)
    }

    override fun setScroll(position: Float) {
        if (!shown()) {
            show()
        } else {
            handler.removeCallbacks(hidePageScrollerRunnable)
        }
    }

    override fun shown() = visibility == View.VISIBLE


    override fun hide() {
        visibility = View.GONE
    }

    override fun show() {
        visibility = View.VISIBLE
    }

    override fun hideDelayed() {
        handler.postDelayed(hidePageScrollerRunnable, 1000)
    }

    override fun setupLayout(pdfView: PDFView) {
        val lp = RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT)
        lp.setMargins(Util.getDP(context, 16), Util.getDP(context, 16), 0, 0)

        pdfView.addView(this, lp)

        tvAll.text = " / ${pdfView.pageCount}"
        tvCurrent.text = "${pdfView.currentPage}"

        this.pdfView = pdfView
    }
}