package com.swiftbridge.converter.mapping.model.mt103;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextBlock {

    private String tag20;
    private String tag32A;
    private List<String> tag50KLines;
    private String tag52A;
    private String tag57A;
    private List<String> tag59Lines;
    private String tag71A;

    public int getTag50KLineCount() {
        return tag50KLines == null ? 0 : tag50KLines.size();
    }

    public int getTag59LineCount() {
        return tag59Lines == null ? 0 : tag59Lines.size();
    }
}
