package one.digitalinnovation.beerstock.service;

import one.digitalinnovation.beerstock.dto.BeerDTO;
import one.digitalinnovation.beerstock.entity.Beer;
import one.digitalinnovation.beerstock.exception.BeerAlreadyRegisteredException;
import one.digitalinnovation.beerstock.mapper.BeerMapper;
import one.digitalinnovation.beerstock.repository.BeerRepository;
import one.digitalinnovation.beerstock.service.builder.BeerDTOBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

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
        BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer beerExpected = beerMapper.toModel(expectedBeerDTO);
        //when
        Mockito.when(beerRepository.findByName(expectedBeerDTO.getName())).thenReturn(Optional.empty());
        Mockito.when(beerRepository.save(beerExpected)).thenReturn(beerExpected);
        //then
        BeerDTO beerCreated = beerService.createBeer(expectedBeerDTO);

        assertThat(beerCreated.getId(), is(equalTo(expectedBeerDTO.getId())));
        assertThat(beerCreated.getName(), is(equalTo(expectedBeerDTO.getName())));
        assertThat(beerCreated.getQuantity(), is(equalTo(expectedBeerDTO.getQuantity())));

        assertThat(beerCreated.getQuantity(), is(greaterThan(2)));
    }

}