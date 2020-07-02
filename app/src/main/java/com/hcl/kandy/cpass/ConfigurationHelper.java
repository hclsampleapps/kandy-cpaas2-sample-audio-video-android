package com.hcl.kandy.cpass;

import com.rbbn.cpaas.mobile.utilities.Configuration;
import com.rbbn.cpaas.mobile.utilities.webrtc.CodecSet;
import com.rbbn.cpaas.mobile.utilities.webrtc.ICEOptions;
import com.rbbn.cpaas.mobile.utilities.webrtc.ICEServers;

public class ConfigurationHelper {

    public static void setConfigurations(String baseUrl) {
        Configuration configuration = Configuration.getInstance();
        configuration.setDTLS(true);
        configuration.setIceOption(ICEOptions.ICE_VANILLA);
        configuration.setICECollectionTimeout(12);
        setPreferedCodecs(baseUrl);
    }

    private static void setPreferedCodecs(String baseUrl) {

        Configuration configuration = Configuration.getInstance();
        ICEServers iceServers = new ICEServers();
        iceServers.addICEServer("turns:turn-ucc-1.genband.com:443?transport=tcp");
        iceServers.addICEServer("turns:turn-ucc-2.genband.com:443?transport=tcp");
        iceServers.addICEServer("stun:turn-ucc-1.genband.com:3478?transport=udp");
        iceServers.addICEServer("stun:turn-ucc-2.genband.com:3478?transport=udp");
        configuration.setICEServers(iceServers);

        CodecSet codecSet = new CodecSet();
        // codecSet.audioCodecs = new CodecSet.AudioCodecType[]{CodecSet.AudioCodecType.AC_G722, CodecSet.AudioCodecType.AC_OPUS};
        // codecSet.videoCodecs = new CodecSet.VideoCodecType[]{CodecSet.VideoCodecType.VC_H264, CodecSet.VideoCodecType.VC_VP9};
        configuration.setPreferredCodecSet(codecSet);
    }
}
