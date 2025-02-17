package io.mosip.registration.controller.reg;

import static io.mosip.registration.constants.LoggerConstants.LOG_REG_SCAN_CONTROLLER;
import static io.mosip.registration.constants.LoggerConstants.LOG_SELECT_LANGUAGE;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import io.mosip.registration.dto.mastersync.GenericDto;
import io.mosip.registration.exception.PreConditionCheckException;
import javafx.scene.control.*;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.controller.BaseController;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.FlowPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

@Controller
public class LanguageSelectionController extends BaseController implements Initializable {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(LanguageSelectionController.class);

	@FXML
	private Button submit;

	@FXML
	private Button cancel;

	@FXML
	private FlowPane checkBoxesPane;

	@FXML
	private Label selectLanguageText;

	@FXML
	private Label errorMessage;

	private Stage popupStage;

	private List<String> selectedLanguages = new ArrayList<>();

	@Autowired
	private PacketHandlerController packetHandlerController;

	@Autowired
	private RegistrationController registrationController;

	public List<String> getSelectedLanguages() {
		return selectedLanguages;
	}

	/**
	 * @return the popupStage
	 */
	public Stage getPopupStage() {
		return popupStage;
	}

	public String getProcessId() {
		return processId;
	}

	public void setProcessId(String processId) {
		this.processId = processId;
	}

	private String processId;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		ResourceBundle resourceBundle = applicationContext.getApplicationLanguageLabelBundle();
		try {
			List<GenericDto> langCodes = getConfiguredLanguages();
			List<String> mandatoryLangCodes = baseService.getMandatoryLanguages();
			int minLangCount = baseService.getMinLanguagesCount();
			int maxLangCount = baseService.getMaxLanguagesCount();

			List<GenericDto> mandatoryLanguages = getConfiguredLanguages(baseService.getMandatoryLanguages());
			String mandatoryLanguagesText = mandatoryLanguages.stream()
					.map(GenericDto::getName)
					.collect(Collectors.joining(RegistrationConstants.COMMA));

			if (mandatoryLanguagesText == null || mandatoryLanguagesText.isEmpty()) {
				mandatoryLanguagesText= resourceBundle.getString("nolanguage");
			}

			String selectLangText = MessageFormat.format(resourceBundle.getString("selectLanguageText"),
					minLangCount, mandatoryLanguagesText);

			selectLanguageText.setText(selectLangText.concat(RegistrationConstants.NEW_LINE));

// Create a ToggleGroup to manage the radio buttons
			ToggleGroup languageToggleGroup = new ToggleGroup();

			for (GenericDto language : langCodes) {
				RadioButton radioButton = new RadioButton();
				radioButton.setId(language.getCode());
				radioButton.setText(language.getName());
				radioButton.getStyleClass().add("languageCheckBox");

				// Add the radio button to the ToggleGroup
				radioButton.setToggleGroup(languageToggleGroup);

				if (language.getCode().equalsIgnoreCase(language.getName())) {
					radioButton.setDisable(true); // If ResourceBundle is not present for configured language, show the radio button but disable it for selection
				} else {
					radioButton.selectedProperty().addListener((options, oldValue, newValue) -> {
						if (newValue) {
							selectedLanguages.add(radioButton.getId());
							if (!mandatoryLangCodes.isEmpty() && mandatoryLangCodes.contains(language.getCode())) {
								errorMessage.setVisible(false);
							}
						} else {
							selectedLanguages.remove(radioButton.getId());
							if (!mandatoryLangCodes.isEmpty()
									&& !CollectionUtils.containsAny(selectedLanguages, mandatoryLangCodes)) {
								errorMessage.setVisible(true);
							}
						}

						// Check if selected languages are as per the min and max lang count
						// Selected languages should contain all the mandatory languages
						if (selectedLanguages.size() >= minLangCount && selectedLanguages.size() <= maxLangCount
								&& (mandatoryLangCodes.isEmpty() || (!mandatoryLangCodes.isEmpty() && selectedLanguages.containsAll(mandatoryLangCodes)))) {
							submit.setDisable(false);
						} else {
							submit.setDisable(true);
						}
					});
				}

				if (language.getCode().equalsIgnoreCase(ApplicationContext.applicationLanguage()) || mandatoryLangCodes.contains(language.getCode())) {
					radioButton.setSelected(true);
				}
				checkBoxesPane.getChildren().add(radioButton);
			}

		} catch (PreConditionCheckException e) {
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_SCAN_POPUP));
		}
	}

	public void init() {
		try {
			LOGGER.info(LOG_SELECT_LANGUAGE, APPLICATION_NAME, APPLICATION_ID,
					"Opening pop-up screen to select language for user registration");

			//this.action = action;
			selectedLanguages.clear();

			popupStage = new Stage();
			popupStage.initStyle(StageStyle.UNDECORATED);

			LOGGER.info(LOG_SELECT_LANGUAGE, APPLICATION_NAME, APPLICATION_ID, "loading SelectLanguage.fxml");
			Parent scanPopup = BaseController.load(getClass().getResource(RegistrationConstants.SELECT_LANGUAGE_PAGE));

			popupStage.setResizable(false);
			Scene scene = new Scene(scanPopup);
			scene.getStylesheets().add(ClassLoader.getSystemClassLoader().getResource(getCssName()).toExternalForm());
			popupStage.setScene(scene);
			popupStage.initModality(Modality.WINDOW_MODAL);
			popupStage.initOwner(fXComponents.getStage());
			popupStage.show();

			LOGGER.info(LOG_SELECT_LANGUAGE, APPLICATION_NAME, APPLICATION_ID,
					"Opening pop-up screen to select language for user registration");

		} catch (IOException ioException) {
			LOGGER.error(LOG_SELECT_LANGUAGE, APPLICATION_NAME, APPLICATION_ID, String.format(
					"%s -> Exception while Opening pop-up screen to select language in user registration  %s -> %s",
					RegistrationConstants.USER_REG_SCAN_EXP, ioException.getMessage(),
					ExceptionUtils.getStackTrace(ioException)));

			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_SCAN_POPUP));
		}
	}

	public void submitLanguages() {
		registrationController.setSelectedLangList(selectedLanguages);
		popupStage.close();
		goToNextPage();
	}

	private void goToNextPage() {
		packetHandlerController.startRegistration(getProcessId());
	}

	public void exitWindow() {
		LOGGER.info(LOG_REG_SCAN_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Calling exit window to close the popup");

		popupStage.close();
		getStage().getScene().getRoot().setDisable(false);

		LOGGER.info(LOG_REG_SCAN_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, "Popup is closed");
	}

	protected CheckBox getCheckBox(String id) {
		return (CheckBox) checkBoxesPane.lookup(RegistrationConstants.HASH + id);
	}

	public void submitLanguagesAndProceed(List<String> langCodes) {
		//this.action = action;
		registrationController.setSelectedLangList(langCodes);
		goToNextPage();
	}

}
