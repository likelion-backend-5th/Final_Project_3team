package com.example.travelers.service;

import com.example.travelers.dao.RedisDao;
import com.example.travelers.dto.BoardDto;
import com.example.travelers.dto.MessageResponseDto;
import com.example.travelers.entity.BoardCategoriesEntity;
import com.example.travelers.entity.BoardsEntity;
import com.example.travelers.entity.CountryEntity;
import com.example.travelers.entity.UsersEntity;
import com.example.travelers.mapping.BoardsMapping;
import com.example.travelers.mapping.BoardsMappingImpl;
import com.example.travelers.repos.BoardCategoriesRepository;
import com.example.travelers.repos.BoardsRepository;
import com.example.travelers.repos.CountryRepository;
import com.example.travelers.repos.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
//@RequiredArgsConstructor
public class BoardsService {
    private final BoardsRepository boardsRepository;
    private final BoardCategoriesRepository boardCategoriesRepository;
    private final CountryRepository countryRepository;
    private final UsersRepository usersRepository;
    private final AuthService authService;
    private final RedisDao redisDao;

    @Autowired
    private MbtiFilter mbtiFilter;

    @Autowired
    public BoardsService(BoardsRepository boardsRepository, BoardCategoriesRepository boardCategoriesRepository, CountryRepository countryRepository, UsersRepository usersRepository, AuthService authService, RedisDao redisDao) {
        this.boardsRepository = boardsRepository;
        this.boardCategoriesRepository = boardCategoriesRepository;
        this.countryRepository = countryRepository;
        this.usersRepository = usersRepository;
        this.authService = authService;
        this.redisDao = redisDao;
    }

    //    @Transactional
    public BoardDto createBoard(BoardDto dto) {

        CountryEntity countryEntity = countryRepository.findById(dto.getCountryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Country not found"));
        BoardCategoriesEntity categoriesEntity = boardCategoriesRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "BoardCategory not found"));

        UsersEntity userEntity = authService.getUser();
//        UsersEntity userEntity = usersRepository.findById(1L)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        BoardsEntity newBoard = BoardsEntity.builder()
                .country(countryEntity)
                .boardCategory(categoriesEntity)
                .user(userEntity)
                .title(dto.getTitle())
                .content(dto.getContent())
                .people(dto.getPeople())
                .status(false)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .createdAt(LocalDateTime.now()).build();

        BoardsEntity savedBoard = boardsRepository.save(newBoard);
        dto.setId(savedBoard.getId());
        return dto;
    }

//    @Cacheable(value = "boards", key = "#id")
    public BoardDto readBoard(Long id) {
        Optional<BoardsEntity> board = boardsRepository.findById(id);
        if (board.isPresent()) {
//            String redisKey = id.toString(); // 해당 글의 ID를 key값으로 선언
//            String redisUserKey = userEntity.getUsername(); // 유저 key
//            String values = redisDao.getValues(redisKey); // 현재 글의 조회수를 가져온다.
//            int views = Integer.parseInt(values); // 가져온 값은 Integer로 변환
//
//            // 유저를 key로 조회한 게시글 ID List안에 해당 게시글 ID가 포함되어있지 않는다면,
//            if (!redisDao.getValuesList(redisUserKey).contains(redisKey)) {
//                redisDao.setValuesList(redisUserKey, redisKey); // 유저 key로 해당 글 ID를 List 형태로 저장
//                views = Integer.parseInt(values) + 1; // 조회수 증가
//                redisDao.setValues(redisKey, String.valueOf(views)); // 글ID key로 조회수 저장
//            }
            BoardDto dto = BoardDto.fromEntity(board.get());
//            dto.setViews(views);
            return dto;
        } else throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Board not found");
    }

    public static long calculateTimeUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = now.truncatedTo(ChronoUnit.DAYS).plusDays(1);
        return ChronoUnit.SECONDS.between(now, midnight);
    } //현재 시간을 기준으로 00시까지의 시간을 계산

    public Page<BoardsMapping> readBoardsAll(Integer pageNumber) {
        UsersEntity userEntity = authService.getUser();
        Pageable pageable = PageRequest.of(pageNumber, 25, Sort.by("id").ascending());
        Page<BoardsEntity> boardsPage = boardsRepository.findAll(pageable);
        List<BoardsMapping> boardsMappings = boardsPage.getContent().stream()
                .map(this::createBoardsMapping)
                .collect(Collectors.toList());
        return new PageImpl<>(boardsMappings, pageable, boardsPage.getTotalElements());
    }

    public Page<BoardsMapping> readBoardsAllByCountryAndCategoryAndMbti(Long countryId, Long categoryId, String mbtiCriteria, Integer pageNumber) {
        UsersEntity userEntity = authService.getUser();
        Optional<UsersEntity> user = usersRepository.findByUsername(userEntity.getUsername());
        if (user.isPresent()) {
            List<String> mbtiList = mbtiFilter.generateMbtiList(mbtiCriteria); // MBTI 리스트 생성

            Pageable pageable = PageRequest.of(pageNumber, 25, Sort.by("id").ascending());

            // 메서드 이름 및 매개변수 수정
            Page<BoardsEntity> boardsPage = boardsRepository.findAllByCountryIdAndBoardCategoryIdAndUser_MbtiIn(countryId, categoryId, mbtiList, pageable);

            List<BoardsMapping> boardsMappings = boardsPage.getContent().stream()
                    .map(this::createBoardsMapping)
                    .collect(Collectors.toList());
            return new PageImpl<>(boardsMappings, pageable, boardsPage.getTotalElements());
        } else throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
    }
    public Page<BoardsMapping> readBoardsAllByCountryAndCategory(Long countryId, Long categoryId, Integer pageNumber) {
        UsersEntity userEntity = authService.getUser();
        Optional<UsersEntity> user = usersRepository.findByUsername(userEntity.getUsername());
        if (user.isPresent()) {
            Pageable pageable = PageRequest.of(pageNumber, 25, Sort.by("id").ascending());
            Page<BoardsEntity> boardsPage = boardsRepository.findAllByCountryIdAndBoardCategoryId(countryId, categoryId, pageable);

            List<BoardsMapping> boardsMappings = boardsPage.getContent().stream()
                    .map(this::createBoardsMapping)
                    .collect(Collectors.toList());
            return new PageImpl<>(boardsMappings, pageable, boardsPage.getTotalElements());
        } else throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
    }

    public Page<BoardsMapping> readBoardsAllByUser(Integer pageNumber) {
        UsersEntity userEntity = authService.getUser();
        Optional<UsersEntity> user = usersRepository.findByUsername(userEntity.getUsername());
        if (user.isPresent()) {
            Pageable pageable = PageRequest.of(pageNumber, 25, Sort.by("id").ascending());
            Page<BoardsEntity> boardsPage = boardsRepository.findAllByUser(user, pageable);

            List<BoardsMapping> boardsMappings = boardsPage.getContent().stream()
                    .map(this::createBoardsMapping)
                    .collect(Collectors.toList());
            return new PageImpl<>(boardsMappings, pageable, boardsPage.getTotalElements());
        } else throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
    }

    public BoardsMapping createBoardsMapping(BoardsEntity boardsEntity) {
        return new BoardsMappingImpl(boardsEntity);
    }

//    @Transactional
    public BoardDto updateBoard(Long id, BoardDto dto) {
        Optional<BoardsEntity> board = boardsRepository.findById(id);
        if (board.isPresent()) {
            CountryEntity countryEntity = countryRepository.findById(dto.getCountryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Country not found"));
            BoardCategoriesEntity categoriesEntity = boardCategoriesRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "BoardCategory not found"));
            UsersEntity userEntity = authService.getUser();
            if (board.get().getUser().getId().equals(userEntity.getId())) {
                BoardsEntity boardsEntity = board.get();
                boardsEntity.setCountry(countryEntity);
                boardsEntity.setBoardCategory(categoriesEntity);
                boardsEntity.setTitle(dto.getTitle());
                boardsEntity.setContent(dto.getContent());
                boardsEntity.setPeople(dto.getPeople());
                boardsEntity.setStartDate(dto.getStartDate());
                boardsEntity.setEndDate(dto.getEndDate());
                boardsEntity.setCreatedAt(LocalDateTime.now());
                return dto.fromEntity(boardsRepository.save(boardsEntity));
            } else throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 게시물이 아닙니다.");
        } else throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Board not found");
    }

//    @Transactional
    public void deleteBoard(Long id) {
        Optional<BoardsEntity> board = boardsRepository.findById(id);
        if (board.isPresent()) {
            UsersEntity userEntity = authService.getUser();
            if (board.get().getUser().getId().equals(userEntity.getId())) {
                BoardsEntity boardsEntity = board.get();
                boardsRepository.delete(boardsEntity);
            } else throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 게시물이 아닙니다.");
        } else throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Board not found");
    }
}