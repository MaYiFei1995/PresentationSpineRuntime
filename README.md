# SpineRuntime-Presentation
基于 spine-libgdx 实现在 AndroidPresentation 上展示 Spine 动画

---
## 效果
- 可以在 Android 的 Presentation 页面上通过`GLSurfaceView`展示 Spine 动画，Spine 动画可以叠加，互不干扰
- 使用[PUSurfaceView](SpineLib/src/main/java/com/mai/spine/PUSurfaceView.kt)替代`PopupWindow`在 Spine 动画的`GLSurfaceView`上层展示其他 View

![演示gif](./imgs/gif.gif)
---
## 注意 
1. Presentation 需要 `android.permission.SYSTEM_ALERT_WINDOW`、`android.permission.WRITE_SETTINGS` 两个权限
2. 虚拟机运行可能报错
3. Spine 动画的缩放适配需要手动修改[SpineBaseAdapter](SpineLib/src/main/java/com/mai/spine/SpineBaseAdapter.kt)，现有的适配需要动画的中心在 (0,-1)
4. Spine 动画对象需要参照 Demo 通过回调依次创建
5. Spine 动画的回调不是主线程，操作 view 需要异步到主线程
6. 新创建的 Spine 动画 View 的 zOrder 始终为 top
---
## 使用
调用请参考 app module 的 [SpinePresentation](app/src/main/java/com/mai/presentationspineruntime/SpinePresentation.kt)
##### 1. 加载GDX
```kotlin
    companion object {
        init {
            GdxNativesLoader.load()
        }
    }
```
##### 2. 创建Adapter
创建自定义 adapter 继承 `SpineBaseAdapter`，在`onCreateImpl()`回调中设置动画数据，在`onCreatedImpl()`中设置 skin 与 animation，也可以在创建 adapter 后设置。
```kotlin
    abstract class SpineBaseAdapter : ApplicationAdapter {

        var tag = "null"    // 用于onCreated异步回调的tag
        var skinName = "default"          // 默认皮肤名称
        var animationName = "animation"   // 默认动画名称
        var debugMode = false               // 默认关闭debug
        var isClickable = true              // view是否可被点击，默认为true

        constructor(padding: Int = 0)
        constructor(paddingStart: Int, paddingTop: Int, paddingEnd: Int, paddingBottom: Int)

        /**
        * 动画的点击回调
        */
        fun setOnSpineClickListener(spineClickListener: OnSpineClickListener)

        /**
        * 动画的创建完成回调
        * 多个Spine动画对象需要依次创建，不可以同时创建多个
        * 多以需要设置创建完成的异步回调，并配合tag来进行判断
        */
        fun setOnCreatedListener(onSpineCreatedListener: OnSpineCreatedListener)

        /**
        * 根据altasPath与skeletonPath初始化之后的回调
        * 用于setSkin、setAnimation、setAttachment
        */
        abstract fun onCreatedImpl()

        /**
        * 点击事件回调
        */
        abstract fun doClick()

        /**
        * 设置Spine的Altas路径
        * @param path 路径
        * @param fileType [FileType]
        */
        fun setAltasPath(path: String, fileType: FileType)

        /**
        * 设置Spine的json文件路径
        */
        fun setSkeletonPath(path: String, fileType: FileType)

        /**
        * 设置动画
        */
        fun setAnimation(trackIndex: Int, animationName: String, loop: Boolean)

        /**
        * 实例开始创建回调
        * 在这里设置Altas与SkeletonPath
        */
        abstract fun onCreateImpl()

        /**
        * 换装饰
        *
        * @param slotName       插槽名称
        * @param attachmentName 装饰名称
        * @return
        */
        fun setAttachment(slotName: String, attachmentName: String): Boolean

        /**
        * 换肤
        *
        * @param skinName 皮肤名称
        * @return
        */
        fun setSkin(skinName: String): Boolean

        /**
        * 创建完成的回调
        */
        interface OnSpineCreatedListener {
            fun onCreated(tag: String)
        }

        /**
        * 创建 SurfaceView 对象
        */
        @Synchronized
        fun create(activity: Activity): View
    }
```
##### 3. 创建 view 与展示
调用 adapter 的`create(activity: Activity)`方法创建 view，并将 view 添加到 parentView 中。

##### 4. 在 Spine 上层绘制纯色与图片
使用[PUSurfaceView](SpineLib/src/main/java/com/mai/spine/PUSurfaceView.kt)可以在 Spine 动画上层绘制纯色与图片。`PUSurfaceView`仅实现了简单的`SurfaceView`绘制功能。
```xml
    <!-- drawType 为 color 时需要配置 bgColor，image 时需要配置 imageSrc -->
    <com.mai.spine.PUSurfaceView
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            app:bgColor="#88000000"
            app:imageSrc="@mipmap/ic_launcher"
            app:drawType="<!-- color / image --> " />
```
##### 5. 动态创建 PUSurfaceView
```kotlin
    class PUSurfaceView : SurfaceView{
        constructor(context: Context, attrs: AttributeSet?)
        constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
        constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int)

        /**
         * 根据传入的color绘制纯色
         */
        fun drawColor(color: Int)

        /**
         * 根据传入id绘制图片
         */
        fun drawImage(id: Int)
    }
```
---
## *TODO*
1. 优化 PUSurfaceView
2. 优化 Spine 动画的缩放适配
3. 优化 Spine 动画的动态 View 创建，不再需要提前在 container 上定义宽高
