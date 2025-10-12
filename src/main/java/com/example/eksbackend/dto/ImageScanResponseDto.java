package com.example.eksbackend.dto;


import lombok.Data;

import java.util.List;

@Data
public class ImageScanResponseDto {




    private ScanDto scanDto;

    List<VulnerabilitiesDto> vulnerabilitiesDtoList;


}
