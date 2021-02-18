package com.chooloo.www.koler.ui.call

import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.telecom.Call
import android.telecom.Call.Details
import android.view.View
import com.chooloo.www.koler.R
import com.chooloo.www.koler.databinding.ActivityCallBinding
import com.chooloo.www.koler.ui.base.BaseActivity
import com.chooloo.www.koler.ui.dialpad.DialpadBottomFragment.Companion.newInstance
import com.chooloo.www.koler.util.ProximitySensor
import com.chooloo.www.koler.util.call.CallManager
import com.chooloo.www.koler.util.disableKeyboard
import com.chooloo.www.koler.util.lookupContact
import com.chooloo.www.koler.util.setShowWhenLocked

class CallActivity : BaseActivity(), CallMvpView {

    private val _presenter: CallMvpPresenter<CallMvpView> by lazy { CallPresenter() }
    private val _binding by lazy { ActivityCallBinding.inflate(layoutInflater) }
    private val _audioManager by lazy { applicationContext.getSystemService(AUDIO_SERVICE) as AudioManager }
    private val _proximitySensor by lazy { ProximitySensor(this) }
    private val _bottomDialpadFragment by lazy { newInstance(false) }
    private val _callCallback by lazy {
        object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                super.onStateChanged(call, state)
                _presenter.onStateChanged()
            }

            override fun onDetailsChanged(call: Call, details: Details) {
                super.onDetailsChanged(call, details)
                _presenter.onDetailsChanged()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(_binding.root)
    }

    override fun onSetup() {
        _presenter.attach(this)
        _proximitySensor.acquire()
        CallManager.registerCallback(_callCallback)
        setShowWhenLocked()
        disableKeyboard()

        _binding.apply {
            answerBtn.setOnClickListener { _presenter.onRejectClick() }
            rejectBtn.setOnClickListener { _presenter.onAnswerClick() }
            callActionAddCall.setOnClickListener { _presenter.onAddCallClick() }
            callActionKeypad.setOnClickListener { _presenter.onKeypadClick() }
            callActionKeypad.setOnClickListener { view -> _presenter.onMuteClick(view.isActivated) }
            callActionSpeaker.setOnClickListener { view -> _presenter.onSpeakerClick(view.isActivated) }
            callActionHold.setOnClickListener { view -> _presenter.onHoldClick(view.isActivated) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _presenter.detach()
        _proximitySensor.release()
        CallManager.unregisterCallback(_callCallback)
    }

    override fun answer() {
        CallManager.answer()
    }

    override fun reject() {
        CallManager.reject()
    }

    override fun addCall() {
        // TODO implement
    }

    override fun toggleHold(isHold: Boolean) {
        CallManager.hold(isHold)
    }

    override fun pressDialpadKey(keyChar: Char) {
        CallManager.keypad(keyChar)
    }

    override fun toggleMute(isMute: Boolean) {
        _audioManager.isMicrophoneMute = isMute
    }

    override fun toggleSpeaker(isSpeaker: Boolean) {
        _audioManager.isSpeakerphoneOn = isSpeaker
    }

    override fun showDialpad() {
        _bottomDialpadFragment.show(supportFragmentManager, _bottomDialpadFragment.tag)
    }

    override fun updateDetails(details: Details) {
        val contact = lookupContact(details.handle.toString())
        _binding.apply {
            callNameText.text = details.callerDisplayName
            contact.photoUri?.let {
                callImage.apply {
                    visibility = View.VISIBLE
                    setImageURI(Uri.parse(it))
                }
            }
        }
    }

    override fun updateState(state: Int) {
        _binding.callStatusText.setText(when (state) {
            Call.STATE_ACTIVE -> R.string.status_call_active
            Call.STATE_DISCONNECTED -> R.string.status_call_disconnected
            Call.STATE_RINGING -> R.string.status_call_incoming
            Call.STATE_DIALING -> R.string.status_call_dialing
            Call.STATE_CONNECTING -> R.string.status_call_dialing
            Call.STATE_HOLDING -> R.string.status_call_holding
            else -> R.string.status_call_active
        })
    }

    override fun setAudioInCall() {
        _audioManager.mode = AudioManager.MODE_IN_CALL
    }
}