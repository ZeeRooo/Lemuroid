package com.swordfish.lemuroid.app.mobile.feature.settings

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.library.LibraryIndexWork
import com.swordfish.lemuroid.app.shared.settings.SettingsInteractor
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import com.zeerooo.wifi.util.WiFiMapper
import dagger.android.support.AndroidSupportInjection
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

class SettingsFragment : PreferenceFragmentCompat() {

    @Inject lateinit var settingsInteractor: SettingsInteractor
    @Inject lateinit var rxSharedPreferences: RxSharedPreferences

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_mobile_settings, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_options_help -> {
                displayLemuroidHelp()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun displayLemuroidHelp() {
        val message = requireContext().getString(R.string.lemuroid_help_content)
        AlertDialog.Builder(requireContext())
            .setMessage(Html.fromHtml(message))
            .show()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.mobile_settings, rootKey)
    }

    override fun onResume() {
        super.onResume()

        val settingsViewModel = ViewModelProviders.of(this, SettingsViewModel.Factory(context!!, rxSharedPreferences))
            .get(SettingsViewModel::class.java)

        val currentDirectory: Preference? = findPreference(getString(R.string.pref_key_extenral_folder))
        val rescanPreference: Preference? = findPreference(getString(R.string.pref_key_rescan))
        val displayBiosPreference: Preference? = findPreference(getString(R.string.pref_key_display_bios_info))

        settingsViewModel.currentFolder
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(scope())
            .subscribe {
                currentDirectory?.summary = getDisplayNameForFolderUri(Uri.parse(it)) ?: getString(R.string.none)
            }

        settingsViewModel.indexingInProgress.observe(this, Observer {
            rescanPreference?.isEnabled = !it
            currentDirectory?.isEnabled = !it
            displayBiosPreference?.isEnabled = !it
        })
    }

    private fun getDisplayNameForFolderUri(uri: Uri) = DocumentFile.fromTreeUri(context!!, uri)?.name

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when (preference?.key) {
            getString(R.string.pref_key_rescan) -> handleRescan()
            getString(R.string.pref_key_extenral_folder) -> handleChangeExternalFolder()
            getString(R.string.pref_key_open_gamepad_bindings) -> handleOpenGamepadBindings()
            getString(R.string.pref_key_display_bios_info) -> handleDisplayBiosInfo()
            "get_esp_ip" -> WiFiMapper(context).getEspIp(activity)
            "set_esp_wifi_credentials" -> setEspCredentials(preference.sharedPreferences.getString("esp_ip", "0.0.0.0"))
        }
        return super.onPreferenceTreeClick(preference)
    }

    private fun handleDisplayBiosInfo() {
        findNavController().navigate(R.id.navigation_settings_bios_info)
    }

    private fun handleOpenGamepadBindings() {
        findNavController().navigate(R.id.navigation_settings_gamepad)
    }

    private fun handleChangeExternalFolder() {
        settingsInteractor.changeLocalStorageFolder()
    }

    private fun handleRescan() {
        context?.let { LibraryIndexWork.enqueueUniqueWork(it) }
    }


    private fun setEspCredentials(espIp: String?) {
        activity?.let {
            val wiFiMapper = WiFiMapper(context)

            val alertDialog: AlertDialog = MaterialAlertDialogBuilder(it, R.style.ThemeOverlay_MaterialComponents_Dialog_Alert).create()

            val view: View = layoutInflater.inflate(R.layout.dialog_esp_credentials, null)

            var ssidInputEditText: TextInputEditText = view.findViewById(R.id.wifi_ssid_input)
            ssidInputEditText?.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_UP)
                    if (event.rawX >= (ssidInputEditText!!.right - ssidInputEditText!!.compoundDrawables[2].bounds.width())) {
                        Toast.makeText(it, "Needed to get current Wi-Fi SSID since Oreo. YOU CAN DISABLE IT LATER IN SETTINGS", Toast.LENGTH_LONG).show()
                        requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 21) // I hate it but seems like the only way to get current WiFi's SSID since Oreo
                    }
                false
            }

            alertDialog.setView(view)
            alertDialog.setTitle("Send Wi-Fi Credentials")
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(android.R.string.ok)) { dialogInterface, i ->
                run {
                    wiFiMapper.sendData("cred" + ssidInputEditText?.text.toString() + '%' + (view.findViewById(R.id.wifi_password_input) as TextInputEditText).text + ';', espIp)
                    Toast.makeText(it, "You CAN DISABLE GPS permission in settings", Toast.LENGTH_LONG).show()
                    wiFiMapper.dispose()
                }
            }
            alertDialog.show()
        }
    }

    @dagger.Module
    class Module
}
