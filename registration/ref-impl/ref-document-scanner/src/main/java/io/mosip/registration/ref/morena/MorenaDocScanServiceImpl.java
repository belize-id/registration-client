package io.mosip.registration.ref.morena;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.gnome.morena.*;
import io.mosip.registration.api.docscanner.DeviceType;
import io.mosip.registration.api.docscanner.DocScannerService;
import io.mosip.registration.api.docscanner.dto.DocScanDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class MorenaDocScanServiceImpl implements DocScannerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MorenaDocScanServiceImpl.class);
    private static final String SERVICE_NAME = "MORENA7";

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public BufferedImage scan(DocScanDevice docScanDevice, String deviceType) {
        if (deviceType != null) {
            Configuration.addDeviceType(deviceType, true);
        }
        Manager manager = Manager.getInstance();
        Optional<Device> result = manager.listDevices().stream()
                .filter(d -> d.toString().equals(docScanDevice.getName()))
                .findFirst();

        if(result.isPresent()) {
            try {
                Device device = result.get();
                if(device instanceof Scanner) {
                    Scanner scanner = (Scanner) device;
                    scanner.setMode(Scanner.RGB_8);
                    LOGGER.info("DPI >>> {}", docScanDevice.getDpi());
                    scanner.setResolution(docScanDevice.getDpi());
                    if(docScanDevice.getFrame() != null && docScanDevice.getFrame().length > 3) {
                        scanner.setFrame(docScanDevice.getFrame()[0], docScanDevice.getFrame()[1],
                                docScanDevice.getFrame()[2], docScanDevice.getFrame()[3]);
                    }
                }

                if(device instanceof Camera) {
                    //No settings
                }

                return MorenaSynchronousHelper.scanImage(device);
            } catch (Exception e) {
                LOGGER.error("Failed to scan", e);
            }
        }
        return null;
    }

    @Override
    public List<DocScanDevice> getConnectedDevices() {
        Manager manager = Manager.getInstance();
        List<Device> connectedDevices = manager.listDevices();
        LOGGER.info("connectedDevices >>> {}", connectedDevices);

        List<DocScanDevice> devices = new ArrayList<>();
        connectedDevices.forEach(device -> {
            DocScanDevice docScanDevice = new DocScanDevice();
            docScanDevice.setId(SERVICE_NAME+":"+device.toString());
            docScanDevice.setName(device.toString());
            docScanDevice.setServiceName(getServiceName());
            docScanDevice.setDeviceType(device instanceof Camera ? DeviceType.CAMERA : DeviceType.SCANNER);
            devices.add(docScanDevice);
        });

        return devices;
    }

    @Override
    public void stop(DocScanDevice docScanDevice) {

    }
}
