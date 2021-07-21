package com.severo.beerstock.service;

import com.severo.beerstock.dto.BeerDTO;
import com.severo.beerstock.entity.Beer;
import com.severo.beerstock.exception.BeerAlreadyRegisteredException;
import com.severo.beerstock.exception.BeerNotFoundException;
import com.severo.beerstock.exception.BeerStockExceededException;
import com.severo.beerstock.mapper.BeerMapper;
import com.severo.beerstock.repository.BeerRepository;
import com.severo.beerstock.service.builder.BeerDTOBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BeerServiceTest {

    private static final long INVALID_BEER_ID = 1L;

    @Mock
    private BeerRepository beerRepository;

    private BeerMapper beerMapper = BeerMapper.INSTANCE;

    @InjectMocks
    private BeerService beerService;

    @Test
    void whenBeerInformedThenItShouldBeCreated() throws BeerAlreadyRegisteredException {
        //given
        BeerDTO expectedBeerDTO = getBeerDTO();
        Beer beerExpected = beerMapper.toModel(expectedBeerDTO);
        //when
        when(beerRepository.findByName(expectedBeerDTO.getName())).thenReturn(Optional.empty());
        when(beerRepository.save(beerExpected)).thenReturn(beerExpected);
        //then
        BeerDTO beerCreated = beerService.createBeer(expectedBeerDTO);

        assertThat(beerCreated.getId(), is(equalTo(expectedBeerDTO.getId())));
        assertThat(beerCreated.getName(), is(equalTo(expectedBeerDTO.getName())));
        assertThat(beerCreated.getQuantity(), is(equalTo(expectedBeerDTO.getQuantity())));

        assertThat(beerCreated.getQuantity(), is(greaterThan(2)));
    }

    @Test
    void whenAlreadyRegisteredBeerInformedThenAnExceptionShouldBeThrown() throws BeerAlreadyRegisteredException {
        //given
        BeerDTO expectedBeerDTO = getBeerDTO();
        Beer beerDuplicated = beerMapper.toModel(expectedBeerDTO);
        //when
        when(beerRepository.findByName(expectedBeerDTO.getName())).thenReturn(Optional.of(beerDuplicated));
        //then
        assertThrows(BeerAlreadyRegisteredException.class, () -> beerService.createBeer(expectedBeerDTO));
    }

    @Test
    void whenValidBeerNameIsGivenThenReturnABeer() throws BeerNotFoundException {
        //given
        BeerDTO expectedFoundBeerDTO = getBeerDTO();
        Beer expectedFoundBeer = beerMapper.toModel(expectedFoundBeerDTO);
        //when
        when(beerRepository.findByName(expectedFoundBeerDTO.getName())).thenReturn(Optional.of(expectedFoundBeer));
        //then
        BeerDTO foundBeerDTO = beerService.findByName(expectedFoundBeerDTO.getName());

        assertThat(foundBeerDTO, is(equalTo(expectedFoundBeerDTO)));
    }

    @Test
    void whenNotRegisteredBeerNameIsGivenThenThrowAnException() {
        //given
        BeerDTO expectedFoundBeerDTO = getBeerDTO();
        //when
        when(beerRepository.findByName(expectedFoundBeerDTO.getName())).thenReturn(Optional.empty());
        //then
        assertThrows(BeerNotFoundException.class, () -> beerService.findByName(expectedFoundBeerDTO.getName()));
    }

    @Test
    void whenListBeerIsCalledThenReturnAListOfBeers() {
        //given
        BeerDTO expectedFoundBeerDTO = getBeerDTO();
        Beer expectedFoundBeer = beerMapper.toModel(expectedFoundBeerDTO);
        //when
        when((beerRepository.findAll())).thenReturn(Collections.singletonList(expectedFoundBeer));
        //then
        List<BeerDTO> foundListBeerDTO = beerService.listAll();

        assertThat(foundListBeerDTO, is(not(empty())));
        assertThat(foundListBeerDTO.get(0), is(equalTo(expectedFoundBeerDTO)));
    }

    @Test
    void whenListBeerIsCalledThenReturnAnEmptyListOfBeers() {
        //given
        //when
        when((beerRepository.findAll())).thenReturn(Collections.EMPTY_LIST);
        //then
        List<BeerDTO> foundListBeerDTO = beerService.listAll();

        assertThat(foundListBeerDTO, is(empty()));
    }

    @Test
    void whenExclusionIsCalledWithValidIdThenABeerShouldBeDeleted() throws BeerNotFoundException {
        //given
        BeerDTO expectedDeletedBeerDTO = getBeerDTO();
        Beer expectedDeletedBeer = beerMapper.toModel(expectedDeletedBeerDTO);
        //when
        when(beerRepository.findById(expectedDeletedBeerDTO.getId())).thenReturn(Optional.of(expectedDeletedBeer));
        doNothing().when(beerRepository).deleteById(expectedDeletedBeerDTO.getId());
        //then
        beerService.deleteById(expectedDeletedBeerDTO.getId());
        verify(beerRepository, times(1)).findById(expectedDeletedBeerDTO.getId());
        verify(beerRepository, times(1)).deleteById(expectedDeletedBeerDTO.getId());
    }

    //TDD
    @Test
    void whenIncrementIsCalledThenIncrementBeersStock() throws BeerNotFoundException, BeerStockExceededException {
        //given
        BeerDTO expectedBeerDTO = getBeerDTO();
        Beer expectedBeer = beerMapper.toModel(expectedBeerDTO);
        //when
        when(beerRepository.findById(expectedBeerDTO.getId())).thenReturn(Optional.of(expectedBeer));
        when(beerRepository.save(expectedBeer)).thenReturn(expectedBeer);
        //then
        int quantityToIncrement = 10;
        int expectedQuantityAfterIncrement = expectedBeerDTO.getQuantity() + quantityToIncrement;
        //then
        BeerDTO incrementedBeerDTO = beerService.increment(expectedBeerDTO.getId(), quantityToIncrement);

        assertThat(expectedQuantityAfterIncrement, equalTo(incrementedBeerDTO.getQuantity()));
        assertThat(expectedQuantityAfterIncrement, lessThan(expectedBeerDTO.getMax()));
    }

    //TDD
    @Test
    void whenIncrementIsGreatherThanMaxThenThrowException() throws BeerNotFoundException, BeerStockExceededException {
        //given
        BeerDTO expectedBeerDTO = getBeerDTO();
        Beer expectedBeer = beerMapper.toModel(expectedBeerDTO);
        //when
        when(beerRepository.findById(expectedBeerDTO.getId())).thenReturn(Optional.of(expectedBeer));
        //then
        int quantityToIncrement = 80;
        //then
        assertThrows(BeerStockExceededException.class, () -> beerService.increment(expectedBeerDTO.getId(), quantityToIncrement));
    }

    //TDD
    @Test
    void whenIncrementAfterSumIsGreatherThanMaxThenThrowException() throws BeerNotFoundException, BeerStockExceededException {
        //given
        BeerDTO expectedBeerDTO = getBeerDTO();
        Beer expectedBeer = beerMapper.toModel(expectedBeerDTO);
        //when
        when(beerRepository.findById(expectedBeerDTO.getId())).thenReturn(Optional.of(expectedBeer));
        //then
        int quantityToIncrement = 45;
        //then
        assertThrows(BeerStockExceededException.class, () -> beerService.increment(expectedBeerDTO.getId(), quantityToIncrement));
    }

    //TDD
    @Test
    void whenIncrementIsCalledWithInvalidIdThenThrowException() throws BeerNotFoundException, BeerStockExceededException {
        //given
        //when
        int quantityToIncrement = 10;
        when(beerRepository.findById(INVALID_BEER_ID)).thenReturn(Optional.empty());
        //then
        assertThrows(BeerNotFoundException.class, () -> beerService.increment(INVALID_BEER_ID, quantityToIncrement));
    }

    //TDD
    @Test
    void whenDecrementIsCalledThenDecrementBeerStock() throws BeerNotFoundException, BeerStockExceededException {
        //given
        BeerDTO expectedBeerDTO = getBeerDTO();
        Beer expectedBeer = beerMapper.toModel(expectedBeerDTO);
        //when
        when(beerRepository.findById(expectedBeerDTO.getId())).thenReturn(Optional.of(expectedBeer));
        when(beerRepository.save(expectedBeer)).thenReturn(expectedBeer);
        //then
        int quantityDecrement = 5;
        int expectedQuantityAfterDecrement = expectedBeerDTO.getQuantity() - quantityDecrement;
        BeerDTO decrementBeerDTO = beerService.decrement(expectedBeerDTO.getId(), quantityDecrement);

        assertThat(expectedQuantityAfterDecrement, equalTo(decrementBeerDTO.getQuantity()));
        assertThat(expectedQuantityAfterDecrement, greaterThan(0));
    }

    @Test
    void whenDecrementIsCalledToEmptyStockThenEmptyBeerStock() throws BeerNotFoundException, BeerStockExceededException {
        //given
        BeerDTO expectedBeerDTO = getBeerDTO();
        Beer expectedBeer = beerMapper.toModel(expectedBeerDTO);
        //when
        when(beerRepository.findById(expectedBeerDTO.getId())).thenReturn(Optional.of(expectedBeer));
        when(beerRepository.save(expectedBeer)).thenReturn(expectedBeer);
        //then
        int quantityDecrement = 10;
        int expectedQuantityAfterDecrement = expectedBeerDTO.getQuantity() - quantityDecrement;
        BeerDTO decrementBeerDTO = beerService.decrement(expectedBeerDTO.getId(), quantityDecrement);

        assertThat(expectedQuantityAfterDecrement, equalTo(0));
        assertThat(expectedQuantityAfterDecrement, equalTo(decrementBeerDTO.getQuantity()));
    }

    //TDD
    @Test
    void whenDecrementIsLowerThanZeroThenThrowException() {
        //given
        BeerDTO expectedBeerDTO = getBeerDTO();
        Beer expectedBeer = beerMapper.toModel(expectedBeerDTO);
        //when
        when(beerRepository.findById(expectedBeerDTO.getId())).thenReturn(Optional.of(expectedBeer));
        //then
        int quantityDecrement = 80;
        //then
        assertThrows(BeerStockExceededException.class, () -> beerService.decrement(expectedBeerDTO.getId(), quantityDecrement));
    }

    @Test
    void whenDecrementIsCalledWithInvalidIdThenThrowException() {
        //given
        //when
        int quantityDecrement = 10;
        when(beerRepository.findById(INVALID_BEER_ID)).thenReturn(Optional.empty());
        //then
        assertThrows(BeerNotFoundException.class, () -> beerService.decrement(INVALID_BEER_ID, quantityDecrement));
    }

    private BeerDTO getBeerDTO() {
        return BeerDTOBuilder.builder().build().toBeerDTO();
    }

}