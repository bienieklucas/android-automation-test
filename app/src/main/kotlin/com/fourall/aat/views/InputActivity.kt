package com.fourall.aat.views

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import com.fourall.aat.Application
import com.fourall.aat.R
import com.fourall.aat.data.di.CommandInjector
import com.fourall.aat.data.local.UserLocalDataSource
import com.fourall.aat.databinding.ActivityInputBinding
import com.fourall.aat.extensions.closeKeyboard
import com.fourall.aat.extensions.isKeyboardOpened
import com.fourall.aat.models.GenericCommand
import com.fourall.aat.repositories.UserDataRepository
import com.fourall.aat.viewmodels.InputViewModel
import com.fourall.aat.viewmodels.factory.InputViewModelFactory
import kotlinx.android.synthetic.main.activity_input.*

class InputActivity : BaseActivity() {

    private lateinit var activityInputBinding: ActivityInputBinding
    private lateinit var inputViewModel: InputViewModel

    private var nameIsOk = false
    private var ageIsOk = false
    private var userId = 0L

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        prepareUi()
        prepareListeners()
        prepareViewModel()
    }

    override fun onResume() {

        super.onResume()

        progressBar.visibility = View.VISIBLE
        inputViewModel.loadUserById(1)

        enableButtonNext(nameIsOk && ageIsOk)
        enableButtonClean(nameIsOk || ageIsOk)
    }

    private fun prepareUi() {

        activityInputBinding = DataBindingUtil.setContentView(this, R.layout.activity_input)

        title = getString(R.string.app_name)
    }

    private fun prepareListeners() {

        userNameTextInputEditText.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(p0: Editable?) {

                nameIsOk = p0?.isNotEmpty() ?: false

                enableButtonNext(ageIsOk && nameIsOk)
                enableButtonClean(nameIsOk || ageIsOk)
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        })

        userAgeTextInputEditText.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(p0: Editable?) {

                ageIsOk = p0?.isNotEmpty() ?: false

                enableButtonNext(nameIsOk && ageIsOk)
                enableButtonClean(nameIsOk || ageIsOk)
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        })

        saveButton.setOnClickListener {

            if (isKeyboardOpened()) closeKeyboard(currentFocus)

            val age = userAgeTextInputEditText.text.toString()
            val name = userNameTextInputEditText.text.toString()

            progressBar.visibility = View.VISIBLE

            inputViewModel.saveUser(name, age)
        }

        cleanButton.setOnClickListener {

            userId = 0
            userNameTextInputEditText.text?.clear()
            userAgeTextInputEditText.text?.clear()
        }
    }

    private fun prepareViewModel() {

        val userRepository = UserDataRepository(UserLocalDataSource(Application.database.userDao()))

        val inputViewModelFactory = InputViewModelFactory(userRepository, CommandInjector)

        inputViewModel = ViewModelProviders.of(this, inputViewModelFactory)
            .get(InputViewModel::class.java)

        inputViewModel.apply {

            command.removeObservers(this@InputActivity)

            command.observe(this@InputActivity, Observer { cmd ->

                cmd?.let { triggerCommand(it) }
            })
        }
    }

    private fun triggerCommand(command: GenericCommand) {

        progressBar.visibility = View.GONE

        when (command) {

            is InputViewModel.Command.ShowUserInfo -> {

                command.user?.let {
                    userId = it.id
                    userNameTextInputEditText.setText(it.name)
                    userAgeTextInputEditText.setText(it.age)
                }
            }

            is InputViewModel.Command.UserSaved -> {
                userId = command.id

                val intent = Intent(this@InputActivity, ResultActivity::class.java)

                intent.putExtra("id", userId)

                startActivity(intent)
            }
        }
    }

    private fun enableButtonNext(enableButton: Boolean) {
        saveButton.isEnabled = enableButton
    }

    private fun enableButtonClean(enableButton: Boolean) {
        cleanButton.isEnabled = enableButton
    }
}