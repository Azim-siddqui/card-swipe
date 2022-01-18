package com.example.testapplication

import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class ThirdFragment:Fragment(){
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_third,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val surfaceView = view.findViewById<SurfaceView>(R.id.surface_view)
        val canvas = surfaceView.holder.lockCanvas()
        canvas?.drawCircle(canvas.width.toFloat().div(2),canvas.height.toFloat().div(2),30f, Paint())
        surfaceView.holder.unlockCanvasAndPost(canvas)
    }
}