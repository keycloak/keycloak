package com.dell.software.ce.dib.claims;

import org.apache.commons.collections4.Transformer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeyTransformer<K> implements Transformer<K, K> {
    private Map<K, K> transforms = null;
    private Map<K, K> regexTransforms = null;

    public KeyTransformer(Map<K, K> transforms) {

        if(transforms != null) {
            this.transforms = transforms;
        }
        else {
            this.transforms = new HashMap<>();
        }

        this.regexTransforms = new HashMap<>();
    }

    public KeyTransformer(List<ClaimMapping<K>> transforms) {
        this.transforms = new HashMap<>();
        this.regexTransforms = new HashMap<>();

        if(transforms != null) {
            for (ClaimMapping<K> mapping : transforms) {
                if (!mapping.isRegex()) {
                    this.transforms.put(mapping.getMatch(), mapping.getReplacement());
                } else {
                    this.regexTransforms.put(mapping.getMatch(), mapping.getReplacement());
                }
            }
        }
    }

    @Override
    public K transform(K key) {
        //always check transforms first as it will be quicker than regex
        if(transforms.containsKey(key)) {
            return transforms.get(key);
        }

        //regex next as it takes longer and we have to try them all to be sure
        for(Map.Entry<K, K> entry : regexTransforms.entrySet()) {
            if(key instanceof String && ((String)key).matches((String)entry.getKey())) {
                return entry.getValue();
            }
        }

        return key;
    }
}
