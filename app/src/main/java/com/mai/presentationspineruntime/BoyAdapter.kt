package com.mai.presentationspineruntime

import com.badlogic.gdx.Files
import com.mai.spine.SpineBaseAdapter

class BoyAdapter : SpineBaseAdapter {

//    override var skinName: String = "default"
//    override var animationName: String = "walk"

    constructor() : super()
    constructor(padding: Int) : super(padding)
    constructor(paddingStart: Int, paddingTop: Int, paddingEnd: Int, paddingBottom: Int) : super(
        paddingStart,
        paddingTop,
        paddingEnd,
        paddingBottom
    )

    override fun onCreateImpl() {
        setAltasPath("spineboy/spineboy.atlas", Files.FileType.Internal)
        setSkeletonPath("spineboy/spineboy.json", Files.FileType.Internal)
    }

    override fun onCreatedImpl(){
        //默认皮肤
        setSkin(skinName)
        //默认动作
        setAnimation(0, "walk", true)
    }

    override fun doClick() {
        mAnimationState.setAnimation(0, "jump", false)
        mAnimationState.addAnimation(0, "walk", true, 1.5f)
    }
}