package dora.widget

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.RadioButton

class DoraRadioGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var currentCheckedId = -1
    private var childOnCheckedChangeListener: CompoundButton.OnCheckedChangeListener? = null
    private var protectFromCheckedChange = false
    private var onCheckedChangeListener: OnCheckedChangeListener? = null
    private var passThroughListener: PassThroughHierarchyChangeListener? = null

    init {
        init()
    }

    private fun init() {
        childOnCheckedChangeListener = CheckedStateTracker()
        passThroughListener = PassThroughHierarchyChangeListener()
        super.setOnHierarchyChangeListener(passThroughListener)
    }

    override fun setOnHierarchyChangeListener(listener: OnHierarchyChangeListener) {
        passThroughListener?.onHierarchyChangeListener = listener
    }

    fun setCheckWithoutNotify(id: Int) {
        if (id != -1 && id == currentCheckedId) {
            return
        }
        protectFromCheckedChange = true
        if (currentCheckedId != -1) {
            setCheckedStateForView(currentCheckedId, false)
        }
        if (id != -1) {
            setCheckedStateForView(id, true)
        }
        currentCheckedId = id
        protectFromCheckedChange = false
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        val buttons = getAllRadioButton(child)
        if (buttons.isNotEmpty()) {
            for (button in buttons) {
                if (button.isChecked) {
                    protectFromCheckedChange = true
                    if (currentCheckedId != -1) {
                        setCheckedStateForView(currentCheckedId, false)
                    }
                    protectFromCheckedChange = false
                    setCheckedId(button.id)
                }
            }
        }
        super.addView(child, index, params)
    }

    private fun getAllRadioButton(child: View): List<RadioButton> {
        val buttons: MutableList<RadioButton> = ArrayList()
        if (child is RadioButton) {
            buttons.add(child)
        } else if (child is ViewGroup) {
            val counts = child.childCount
            for (i in 0 until counts) {
                buttons.addAll(getAllRadioButton(child.getChildAt(i)))
            }
        }
        return buttons
    }

    fun check(id: Int) {
        if (id != -1 && id == currentCheckedId) {
            return
        }
        if (currentCheckedId != -1) {
            setCheckedStateForView(currentCheckedId, false)
        }
        if (id != -1) {
            setCheckedStateForView(id, true)
        }
        setCheckedId(id)
    }

    private fun setCheckedId(id: Int) {
        currentCheckedId = id
        onCheckedChangeListener?.onCheckedChanged(this, currentCheckedId)
    }

    private fun setCheckedStateForView(viewId: Int, checked: Boolean) {
        val checkedView = findViewById<View>(viewId)
        if (checkedView != null && checkedView is RadioButton) {
            checkedView.isChecked = checked
        }
    }

    fun getCheckedRadioButtonId(): Int {
        return currentCheckedId
    }

    fun clearCheck() {
        check(-1)
    }

    fun setOnCheckedChangeListener(listener: OnCheckedChangeListener) {
        onCheckedChangeListener = listener
    }

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
        return LayoutParams(context, attrs)
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams): Boolean {
        return p is LayoutParams
    }

    override fun generateDefaultLayoutParams(): LinearLayout.LayoutParams {
        return LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onInitializeAccessibilityEvent(event: AccessibilityEvent) {
        super.onInitializeAccessibilityEvent(event)
        event.className = DoraRadioGroup::class.java.name
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = DoraRadioGroup::class.java.name
    }

    interface OnCheckedChangeListener {
        fun onCheckedChanged(group: DoraRadioGroup?, checkedId: Int)
    }

    class LayoutParams : LinearLayout.LayoutParams {
        constructor(c: Context?, attrs: AttributeSet?) : super(c, attrs)
        constructor(w: Int, h: Int) : super(w, h)
        constructor(w: Int, h: Int, initWeight: Float) : super(w, h, initWeight)
        constructor(p: ViewGroup.LayoutParams?) : super(p)
        constructor(source: MarginLayoutParams?) : super(source)

        override fun setBaseAttributes(
            a: TypedArray,
            widthAttr: Int, heightAttr: Int
        ) {
            width = if (a.hasValue(widthAttr)) {
                a.getLayoutDimension(widthAttr, "layout_width")
            } else {
                WRAP_CONTENT
            }
            height = if (a.hasValue(heightAttr)) {
                a.getLayoutDimension(heightAttr, "layout_height")
            } else {
                WRAP_CONTENT
            }
        }
    }

    private inner class CheckedStateTracker : CompoundButton.OnCheckedChangeListener {
        override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
            if (protectFromCheckedChange) {
                return
            }
            protectFromCheckedChange = true
            if (currentCheckedId != -1) {
                setCheckedStateForView(currentCheckedId, false)
            }
            protectFromCheckedChange = false
            val id = buttonView.id
            setCheckedId(id)
        }
    }

    private inner class PassThroughHierarchyChangeListener : OnHierarchyChangeListener {

        var onHierarchyChangeListener: OnHierarchyChangeListener? = null

        override fun onChildViewAdded(parent: View, child: View) {
            if (parent === this@DoraRadioGroup) {
                val buttons = getAllRadioButton(child)
                if (buttons.isNotEmpty()) {
                    for (btn in buttons) {
                        var id = btn.id
                        // generates an id if it's missing
                        if (id == NO_ID) {
                            id = generateViewId()
                            btn.id = id
                        }
                        btn.setOnCheckedChangeListener(childOnCheckedChangeListener)
                    }
                }
            }
            onHierarchyChangeListener?.onChildViewAdded(parent, child)
        }

        override fun onChildViewRemoved(parent: View, child: View) {
            if (parent === this@DoraRadioGroup) {
                val buttons = getAllRadioButton(child)
                if (buttons.isNotEmpty()) {
                    for (button in buttons) {
                        button.setOnCheckedChangeListener(null)
                    }
                }
            }
            onHierarchyChangeListener?.onChildViewRemoved(parent, child)
        }
    }
}