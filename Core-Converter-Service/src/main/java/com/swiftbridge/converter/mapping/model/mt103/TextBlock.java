package com.swiftbridge.converter.mapping.model.mt103;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.List;
import java.util.Objects;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class TextBlock {

    private String tag20;
    private String tag32A;
    @Singular("tag50KLine")
    private List<String> tag50KLines;
    private String tag52A;
    private String tag57A;
    @Singular("tag59Line")
    private List<String> tag59Lines;
    private String tag71A;

    public int getTag50KLineCount() {
        return tag50KLines == null ? 0 : tag50KLines.size();
    }

    public int getTag59LineCount() {
        return tag59Lines == null ? 0 : tag59Lines.size();
    }

    public void validateMandatoryTags() {
        Objects.requireNonNull(tag20, "tag20 is required");
        Objects.requireNonNull(tag32A, "tag32A is required");
        Objects.requireNonNull(tag59Lines, "tag59 is required");
        Objects.requireNonNull(tag71A, "tag71A is required");
    }

    public void validateLineAndFieldLimits() {
        if (tag20 != null && tag20.length() > 16) {
            throw new IllegalArgumentException("tag20 max length is 16");
        }
        if (tag50KLines != null && tag50KLines.size() > 4) {
            throw new IllegalArgumentException("tag50K supports up to 4 lines");
        }
        if (tag59Lines != null && tag59Lines.size() > 4) {
            throw new IllegalArgumentException("tag59 supports up to 4 lines");
        }
    }
}
