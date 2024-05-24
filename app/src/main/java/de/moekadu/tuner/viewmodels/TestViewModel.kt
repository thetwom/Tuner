package de.moekadu.tuner.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

class TestData @Inject constructor(){
    var a by mutableStateOf("Hello World")
}

@HiltViewModel
class TestViewModel @Inject constructor(
    val test: TestData
) : ViewModel(){


}