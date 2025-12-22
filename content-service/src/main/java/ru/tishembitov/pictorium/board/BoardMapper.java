package ru.tishembitov.pictorium.board;

import org.mapstruct.*;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BoardMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "pins", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Board toEntity(BoardCreateRequest request, String userId);

    BoardResponse toResponse(Board board);

    List<BoardResponse> toResponseList(List<Board> boards);

    default BoardWithPinStatusResponse toWithPinStatusResponse(BoardWithPinStatusProjection projection) {
        return new BoardWithPinStatusResponse(
                projection.getId(),
                projection.getUserId(),
                projection.getTitle(),
                projection.getCreatedAt(),
                projection.getUpdatedAt(),
                projection.getHasPin(),
                projection.getPinCount()
        );
    }

    default List<BoardWithPinStatusResponse> toWithPinStatusResponseList(
            List<BoardWithPinStatusProjection> projections) {
        return projections.stream()
                .map(this::toWithPinStatusResponse)
                .toList();
    }
}