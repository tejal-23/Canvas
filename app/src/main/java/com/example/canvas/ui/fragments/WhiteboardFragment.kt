package com.example.canvas.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.canvas.R
import com.example.canvas.viewmodels.WhiteboardViewModel
import com.example.canvas.views.WhiteboardView
import kotlinx.coroutines.launch

class WhiteboardFragment : Fragment() {

    private val viewModel: WhiteboardViewModel by activityViewModels()
    private lateinit var whiteboardView: WhiteboardView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_whiteboard, container, false).apply {
            whiteboardView = findViewById(R.id.whiteboardCanvas)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe ViewModel state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                whiteboardView.setState(state)
            }
        }

        // Hook up callbacks from WhiteboardView -> ViewModel
        whiteboardView.onStrokeCompleted = { stroke ->
            viewModel.addStroke(stroke)
        }
        whiteboardView.onShapeInserted = { shape ->
            viewModel.addShape(shape)
        }
        whiteboardView.onTextInserted = { text ->
            viewModel.addText(text)
        }

        // Toolbar buttons
        view.findViewById<View>(R.id.btnEraser).setOnClickListener {
            viewModel.enableEraser(true)
        }
        view.findViewById<View>(R.id.btnPen).setOnClickListener {
            viewModel.enableEraser(false)
        }
        view.findViewById<View>(R.id.btnUndo).setOnClickListener {
            viewModel.undo()
        }
        view.findViewById<View>(R.id.btnClear).setOnClickListener {
            viewModel.clearCanvas()
        }
        view.findViewById<View>(R.id.btnRedo).setOnClickListener {
            viewModel.redo()
        }

    }
}
